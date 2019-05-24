#!/bin/bash
# Use for deploying pre-built images from gcr.io/coolretailer/* from the interactive setup
# APIs need to be enabled **BEFORE** running this script (done in the interactive setup)
# For complete independent setup from source code with Google Cloud Build use setup.sh
#
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

bold "Creating a service account $SERVICE_ACCOUNT_NAME..."

gcloud iam service-accounts create \
  $SERVICE_ACCOUNT_NAME \
  --display-name $SERVICE_ACCOUNT_NAME


SA_EMAIL=$(gcloud iam service-accounts list \
  --filter="displayName:$SERVICE_ACCOUNT_NAME" \
  --format='value(email)')

bold "Waiting 10s for service account to be active..."
sleep 10

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

bold "Importing data..."
wget https://raw.githubusercontent.com/BestBuyAPIs/open-data-set/master/products.json

docker run \
  -v "$PWD":/tmp \
  -it gcr.io/coolretailer/queryservice:latest \
  --spring.profiles.active=JSON \
  --input.json=/tmp/products.json \
  --exit

gsutil mb -p $PROJECT_ID -l europe-west4 $BUCKET_URI
gsutil cp products.nd.json $BUCKET_URI
bq --location=EU mk -d coolretailer
bq --location=EU load --autodetect --source_format=NEWLINE_DELIMITED_JSON coolretailer.products $BUCKET_URI/products.nd.json

bold "Creating cluster..."
gcloud beta container clusters create $GKE_CLUSTER \
  --zone $ZONE \
  --username "admin" \
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

rm ./app-gac.json

bold "Create role bindings..."
kubectl create clusterrolebinding cluster-admin-binding \
 --clusterrole=cluster-admin \
 --user=$(gcloud config get-value core/account)
kubectl create clusterrolebinding kiali \
 --clusterrole=cluster-admin \
 --user=$(gcloud config get-value core/account)



bold "Installing SRE stack..."
wget -qO- https://github.com/istio/istio/releases/download/1.0.6/istio-1.0.6-linux.tar.gz | tar xvz
cd istio-1.0.6
kubectl label namespace default istio-injection=enabled

bold "Install Prometheus..."
kubectl apply -n istio-system -f https://storage.googleapis.com/gke-release/istio/release/1.0.6-gke.3/patches/install-prometheus.yaml

bold "Install Helm..."
wget -P ./helm https://storage.googleapis.com/kubernetes-helm/helm-v2.11.0-linux-amd64.tar.gz
tar xf ./helm/helm-v2.11.0-linux-amd64.tar.gz  -C ./helm/
export PATH="$PATH:./istio-1.0.6/bin::./helm/linux-amd64/"

bold "Install Grafana..."

helm template --set grafana.enabled=false --namespace istio-system install/kubernetes/helm/istio > off.yaml
helm template --set grafana.enabled=true --namespace istio-system install/kubernetes/helm/istio > on.yaml
diff --line-format=%L on.yaml off.yaml > grafana.yaml
kubectl apply -n istio-system -f grafana.yaml

bold "Install Kiali..."

wget -O install/kubernetes/helm/istio/charts/kiali/templates/clusterrole.yaml https://raw.githubusercontent.com/kiali/kiali/v0.13/deploy/kubernetes/clusterrole.yaml
sed -i "s/\${VERSION_LABEL}/master/g" install/kubernetes/helm/istio/charts/kiali/templates/clusterrole.yaml

kubectl apply -n istio-system -f ../istio-manifests/kiali-secrets.yaml

helm template install/kubernetes/helm/istio/charts/kiali \
  --name kiali \
  --namespace istio-system \
  --set global.imagePullSecrets= \
  --set dashboard.secretName=kiali \
  --set dashboard.usernameKey=username  \
  --set dashboard.passphraseKey=passphrase  \
  --set ingress.enabled=false \
  --set hub=docker.io/kiali \
  --set dashboard.grafanaURL=http://grafana:3000 \
  --set tag=v0.13  > kiali.yaml

kubectl apply -n istio-system -f kiali.yaml
cd ..

kubectl apply -f ./istio-manifests/

bold "Patching manifests..."
sed -i "s/TAG_NAME/latest/g" ./kubernetes-manifests/*.yaml
sed -i "s/REDIS_HOST/redis-master/g" ./kubernetes-manifests/*.yaml
# Only for image
sed -i "s/\/_PROJECT_ID/\/coolretailer/g" ./kubernetes-manifests/*.yaml
sed -i "s/_PROJECT_ID/$PROJECT_ID/g" ./kubernetes-manifests/*.yaml

bold "Starting deployments..."
kubectl apply -f ./kubernetes-manifests/redis/

kubectl apply -f ./kubernetes-manifests/dataservice.yaml
kubectl rollout status deployment.extensions/dataservice

kubectl apply -f ./kubernetes-manifests/cacheservice.yaml
kubectl rollout status deployment.extensions/cacheservice

kubectl apply -f ./kubernetes-manifests/queryservice.yaml
kubectl rollout status deployment.extensions/queryservice

kubectl apply -f ./kubernetes-manifests/loadcache.yaml

kubectl apply -f ./kubernetes-manifests/loadgenerator.yaml
kubectl rollout status deployment.extensions/loadgenerator

bold "Deploying front end"
sed -i "s/_ENDPOINT/`kubectl -n istio-system get service istio-ingressgateway -o jsonpath={.status.loadBalancer.ingress[0].ip}|tail -1`/g" ../src/ui/static/ux.js
gcloud app create --region=europe-west3
gsutil -m cp -r ../src/ui/static/lib gs://$PROJECT_ID.appspot.com/coolretailer/lib
gsutil -m cp ../src/ui/static/ux.* gs://$PROJECT_ID.appspot.com/coolretailer
gsutil -m acl -r ch -u AllUsers:R gs://$PROJECT_ID.appspot.com/coolretailer
bold "Deployment complete!"
bold "Application url: http://storage.googleapis.com/$PROJECT_ID.appspot.com/coolretailer/ux.html"
bold "API Endpoint: http://`kubectl -n istio-system get service istio-ingressgateway -o jsonpath={.status.loadBalancer.ingress[0].ip}|tail -1`/api/fetchProducts?name=go"