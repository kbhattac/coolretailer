#!/bin/sh
cd ~
gcloud beta container \
    --project "coolretailer" clusters create "ux" \
    --zone "europe-west4-a" \
    --username "admin" \
    --cluster-version "1.10.9-gke.7" \
    --machine-type "n1-highcpu-4" \
    --image-type "COS" \
    --disk-type "pd-standard" \
    --disk-size "100" \
    --scopes "https://www.googleapis.com/auth/cloud-platform" \
    --num-nodes "3" \
    --enable-stackdriver-kubernetes \
    --no-enable-ip-alias \
    --network "projects/coolretailer/global/networks/default" \
    --subnetwork "projects/coolretailer/regions/europe-west4/subnetworks/default" \
    --enable-autoscaling \
    --min-nodes "3" \
    --max-nodes "10" \
    --addons HorizontalPodAutoscaling,HttpLoadBalancing,Istio \
    --istio-config auth=MTLS_PERMISSIVE \
    --enable-autoupgrade \
    --enable-autorepair


kubectl create clusterrolebinding cluster-admin-binding \
 --clusterrole=cluster-admin \
 --user=$(gcloud config get-value core/account)

gcloud container clusters get-credentials ux --zone europe-west4-a --project coolretailer

kubectl create secret generic app-gac --from-file=app-gac.json

kubectl apply -f CoolRetailer/setup/kubernetes-manifests
watch 'kubectl get pods'
