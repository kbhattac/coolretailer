#!/bin/bash

bold() {
  echo ". $(tput bold)" "$*" "$(tput sgr0)";
}

err() {
  echo "$*" >&2;
}

source ./properties

if [ -z "$PROJECT_ID" ]; then
  err "Not running in a GCP project. Please run gcloud config set project $PROJECT_ID."
  exit 1
fi

if [ -z "$CLOUD_BUILD_EMAIL" ]; then
  err "Cloud Build email is empty. Exiting."
  exit 1
fi

bold "Starting the setup process in project $PROJECT_ID..."
bold "Enable APIs..."
gcloud services enable \
  container.googleapis.com \
  containeranalysis.googleapis.com \
  cloudkms.googleapis.com \
  cloudbuild.googleapis.com \
  sourcerepo.googleapis.com \
  cloudtrace.googleapis.com \
  logging.googleapis.com \
  monitoring.googleapis.com

bold "Creating a service account $SERVICE_ACCOUNT_NAME..."

gcloud iam service-accounts create \
  $SERVICE_ACCOUNT_NAME \
  --display-name $SERVICE_ACCOUNT_NAME

SA_EMAIL=$(gcloud iam service-accounts list \
  --filter="displayName:$SERVICE_ACCOUNT_NAME" \
  --format='value(email)')
  
if [ -z "$SA_EMAIL" ]; then
  err "Service Account email is empty. Exiting."
  exit 1
fi

bold "Adding policy binding to $SERVICE_ACCOUNT_NAME email: $SA_EMAIL"
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/bigquery.dataViewer
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/bigquery.jobUser
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/clouddebugger.agent
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/cloudprofiler.agent
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/cloudtrace.agent
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/contextgraph.asserter
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/errorreporting.admin
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/logging.logWriter
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/monitoring.metricWriter

bold "Building application..."
git clone --branch $VERSION https://github.com/kbhattac/coolretailer.git
cd coolretailer
mvn clean install -DskipTests -f src

bold "Importing data..."
wget https://raw.githubusercontent.com/BestBuyAPIs/open-data-set/master/products.json
java -jar src/queryservice/target/queryservice-1.0.0.jar --spring.profiles.active=JSON --input.json="$PWD"/products.json --exit
gsutil mb -p $PROJECT_ID -l europe-west4 $BUCKET_URI
gsutil cp products.nd.json $BUCKET_URI
bq --location=EU mk -d coolretailer
bq --location=EU load --autodetect --source_format=NEWLINE_DELIMITED_JSON coolretailer.products $BUCKET_URI/products.nd.json
mvn clean -DskipTests -f src

bold "Creating cluster..."
gcloud beta container clusters create $GKE_CLUSTER \
  --zone $ZONE \
  --username "admin" \
  --cluster-version "1.10.11-gke.1" \
  --machine-type "n1-standard-2" \
  --image-type "COS" \
  --disk-type "pd-standard" \
  --disk-size "100" \
  --scopes "https://www.googleapis.com/auth/cloud-platform" \
  --num-nodes "3" \
  --enable-stackdriver-kubernetes \
  --enable-ip-alias \
  --enable-autoscaling \
  --max-nodes "10" \
  --addons HorizontalPodAutoscaling,HttpLoadBalancing,Istio \
  --istio-config=auth=MTLS_PERMISSIVE \
  --enable-autoupgrade \
  --enable-autorepair
gcloud container clusters get-credentials $GKE_CLUSTER --zone $ZONE

bold "Install service account secret..."
gcloud iam service-accounts keys create ./app-gac.json \
  --iam-account $SA_EMAIL

kubectl create secret generic app-gac --from-file=./app-gac.json

gcloud kms keyrings create coolretailer --location=global

gcloud kms keys create app-gac \
      --location global \
      --keyring coolretailer \
      --purpose encryption

gcloud kms encrypt \
    --location=global  \
    --keyring=coolretailer \
    --key=app-gac \
    --plaintext-file=app-gac.json \
    --ciphertext-file=app-gac.json.enc

rm ./app-gac.json

bold "Adding policy binding to cloudbuilder: $CLOUD_BUILD_EMAIL"

gcloud kms keys add-iam-policy-binding \
    app-gac --location=global --keyring=coolretailer \
    --member=serviceAccount:$CLOUD_BUILD_EMAIL \
    --role=roles/cloudkms.cryptoKeyDecrypter

bold "Create role bindings..."
kubectl create clusterrolebinding cluster-admin-binding \
 --clusterrole=cluster-admin \
 --user=$(gcloud config get-value core/account)
kubectl create clusterrolebinding kiali \
 --clusterrole=cluster-admin \
 --user=$(gcloud config get-value core/account)



bold "Installing SRE stack..."
wget -qO- https://github.com/istio/istio/releases/download/1.0.3/istio-1.0.3-linux.tar.gz | tar xvz
cd istio-1.0.3
kubectl label namespace default istio-injection=enabled
kubectl apply -f https://storage.googleapis.com/gke-release/istio/release/1.0.3-gke.0/stackdriver/stackdriver-tracing.yaml
kubectl apply -f https://storage.googleapis.com/gke-release/istio/release/1.0.3-gke.0/stackdriver/stackdriver-logs.yaml
kubectl apply -n istio-system -f https://storage.googleapis.com/gke-release/istio/release/1.0.3-gke.0/patches/install-prometheus.yaml
wget -O ./install-grafana.yaml https://storage.googleapis.com/gke-release/istio/release/1.0.3-gke.0/patches/install-grafana.yaml
sed -i.bak s/prometheus:9090/prometheus-user:9090/g install-grafana.yaml
kubectl apply -n istio-system -f install-grafana.yaml

wget -O install/kubernetes/helm/istio/charts/kiali/templates/clusterrole.yaml https://raw.githubusercontent.com/kiali/kiali/v0.13/deploy/kubernetes/clusterrole.yaml
sed -i "s/\${VERSION_LABEL}/master/g" install/kubernetes/helm/istio/charts/kiali/templates/clusterrole.yaml

bold "Install Helm..."
wget -P ./helm https://storage.googleapis.com/kubernetes-helm/helm-v2.11.0-linux-amd64.tar.gz
tar xf ./helm/helm-v2.11.0-linux-amd64.tar.gz  -C ./helm/
export PATH="$PATH:./istio-1.0.3/bin::./helm/linux-amd64/"

helm template install/kubernetes/helm/istio/charts/kiali \
  --name kiali \
  --namespace istio-system \
  --set global.imagePullSecrets= \
  --set dashboard.username=admin \
  --set dashboard.passphrase=admin  \
  --set ingress.enabled=false \
  --set hub=docker.io/kiali \
  --set dashboard.grafanaURL=http://grafana:3000 \
  --set tag=v0.13  > kiali.yaml
sed -i.bak s/prometheus:9090/prometheus-user:9090/g kiali.yaml
kubectl apply -n istio-system -f kiali.yaml
cd ..
bold "Starting build pipeline..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member=serviceAccount:$CLOUD_BUILD_EMAIL \
    --role=roles/container.admin
gcloud auth configure-docker -q
echo $'#!include:.gitignore\n!.git\n!src\ntarget/' > .gcloudignore
kubectl apply -f setup/istio-manifests/
gcloud builds submit --config=build/cloudbuild.yaml --timeout=20m --substitutions COMMIT_SHA=$COMMIT_SHA .

bold "Patching manifests..."
sed -i "s,TAG_NAME,$COMMIT_SHA," ./setup/kubernetes-manifests/*.yaml
sed -i "s,REDIS_HOST,redis-master," ./setup/kubernetes-manifests/*.yaml
sed -i "s,_PROJECT_ID,$PROJECT_ID," ./setup/kubernetes-manifests/*.yaml

bold "Starting deployments..."
kubectl apply -f ./setup/kubernetes-manifests/redis/

kubectl apply -f ./setup/kubernetes-manifests/dataservice.yaml
kubectl rollout status deployment.extensions/dataservice

kubectl apply -f ./setup/kubernetes-manifests/cacheservice.yaml
kubectl rollout status deployment.extensions/cacheservice

kubectl apply -f ./setup/kubernetes-manifests/queryservice.yaml
kubectl rollout status deployment.extensions/queryservice

kubectl apply -f ./setup/kubernetes-manifests/loadcache.yaml

kubectl apply -f ./setup/kubernetes-manifests/loadgenerator.yaml
kubectl rollout status deployment.extensions/loadgenerator

bold "Deploying front end"
sed -i "s/_ENDPOINT/`kubectl -n istio-system get service istio-ingressgateway -o jsonpath={.status.loadBalancer.ingress[0].ip}|tail -1`/g" ./src/ui/static/ux.js
gcloud app create --region=europe-west3
gsutil -m cp -r ./src/ui/static/lib gs://$PROJECT_ID.appspot.com/coolretailer/lib
gsutil -m cp ./src/ui/static/ux.* gs://$PROJECT_ID.appspot.com/coolretailer
gsutil -m acl -r ch -u AllUsers:R gs://$PROJECT_ID.appspot.com/coolretailer
bold "Deployment complete!"
bold "Application url: http://storage.googleapis.com/$PROJECT_ID.appspot.com/coolretailer/ux.html"
bold "API Endpoint: http://`kubectl -n istio-system get service istio-ingressgateway -o jsonpath={.status.loadBalancer.ingress[0].ip}|tail -1`/api/fetchProducts?name=go"