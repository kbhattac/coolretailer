#!/bin/bash
source properties
gcloud container clusters get-credentials $GKE_CLUSTER --zone $ZONE --project $PROJECT_ID
kubectl port-forward $(kubectl get pod --selector="app=loadgenerator,tier=frontend" --output jsonpath='{.items[0].metadata.name}') 8080:8089 >> /dev/null &
kubectl port-forward --namespace istio-system $(kubectl get pod --namespace istio-system --selector="app=kiali" --output jsonpath='{.items[0].metadata.name}') 8081:20001 >> /dev/null &
kubectl port-forward --namespace istio-system $(kubectl get pod --namespace istio-system --selector="app=grafana" --output jsonpath='{.items[0].metadata.name}') 8082:3000 >> /dev/null &