#!/bin/bash

bold() {
  echo ". $(tput bold)" "$*" "$(tput sgr0)";
}

err() {
  echo "$*" >&2;
}

source ./properties

if [ -z "$PROJECT_ID" ]; then
  err "Not running in a GCP project. Exiting."
  exit 1
fi

if [ -z "$CLOUD_BUILD_EMAIL" ]; then
  err "Cloud Build email is empty. Exiting."
  exit 1
fi

if [ -z "$SA_EMAIL" ]; then
  err "Service Account email is empty. Exiting."
  exit 1
fi

bold "Removing Kuberentes Admin role from $CLOUD_BUILD_EMAIL..."
gcloud projects remove-iam-policy-binding $PROJECT_ID \
    --member=serviceAccount:$CLOUD_BUILD_EMAIL \
    --role=roles/container.admin

bold "Removing roles from $SA_EMAIL..."
gcloud projects remove-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/bigquery.dataViewer
gcloud projects remove-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/bigquery.jobUser
gcloud projects remove-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/clouddebugger.agent
gcloud projects remove-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/cloudprofiler.agent
gcloud projects remove-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/cloudtrace.agent
gcloud projects remove-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/contextgraph.asserter
gcloud projects remove-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/errorreporting.admin
gcloud projects remove-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/logging.logWriter
gcloud projects remove-iam-policy-binding $PROJECT_ID \
  --member serviceAccount:$SA_EMAIL \
  --role roles/monitoring.metricWriter

bold "Deleting service account $SERVICE_ACCOUNT_NAME..."
gcloud iam service-accounts delete $SERVICE_ACCOUNT_NAME@$PROJECT_ID.iam.gserviceaccount.com --quiet

bold "Deleting GKE cluster $GKE_CLUSTER in zone $ZONE"
gcloud beta container clusters delete $GKE_CLUSTER --zone $ZONE --quiet
bold "Deleting GCR images"
gcloud container images delete gcr.io/$PROJECT_ID/queryservice --force-delete-tags --quiet
gcloud container images delete gcr.io/$PROJECT_ID/cacheservice --force-delete-tags --quiet
gcloud container images delete gcr.io/$PROJECT_ID/dataservice --force-delete-tags --quiet
gcloud container images delete gcr.io/$PROJECT_ID/loadgenerator --force-delete-tags --quiet
bold "Deleting GCS bucket $BUCKET_URI"
gsutil rm -r $BUCKET_URI
bold "Deleting BigQuery dataset coolretailer..."
bq rm -r --force coolretailer
bold "Deleting Front End bucket $PROJECT_ID.appspot.com/coolretailer ..."
gsutil rm -r gs://$PROJECT_ID.appspot.com/coolretailer
bold "Uninstallation complete!"