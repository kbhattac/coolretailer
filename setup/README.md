# CoolRetailer Interactive setup
<walkthrough-tutorial-url url="https://github.com/kbhattac/coolretailer/blob/master/README.md"></walkthrough-tutorial-url>

## Introduction

This tutorial creates, builds and installs the complete stack in an interactive way.

**Time to complete**: 20 mins

Click the **Next** button to move to the next step.

## Project setup

Google Cloud Platform organises resources into projects. This allows you to
collect all of the related resources for a single application in one place.

<walkthrough-project-billing-setup key="project-id"></walkthrough-project-billing-setup>

    gcloud config set project {{project-id}}

## Enable API access

This tutorial will require the use of the following APIs. Please enable them and click on Next to continue.

<walkthrough-enable-apis apis="container.googleapis.com,containeranalysis.googleapis.com,cloudkms.googleapis.com,cloudbuild.googleapis.com,sourcerepo.googleapis.com,cloudtrace.googleapis.com,logging.googleapis.com,monitoring.googleapis.com">
</walkthrough-enable-apis>

    gcloud services enable container.googleapis.com containeranalysis.googleapis.com cloudkms.googleapis.com cloudbuild.googleapis.com sourcerepo.googleapis.com cloudtrace.googleapis.com logging.googleapis.com monitoring.googleapis.com

## Installation

This <walkthrough-editor-open-file filePath="coolretailer/setup/deploy.sh">script</walkthrough-editor-open-file> sets up the complete application and monitoring stack. At the end note the **Application URL** and **API endpoint** for testing the application.

**Time to complete**: 15 mins

    ./deploy.sh

## Review application

Setup tunnels to access the following: 

    tunnels/connect.sh

[Locust load generator(8080)](https://ssh.cloud.google.com/devshell/proxy?authuser=0&port=8080)

[Kiali Dashboard (admin/admin) (8081)](https://ssh.cloud.google.com/devshell/proxy?authuser=0&port=8081)

[Grafana Dashboard(8082)](https://ssh.cloud.google.com/devshell/proxy?authuser=0&port=8082)

- In Locust dashboard enter:

    `Number of users to simulate` = 10,
    `hatch rate` = 5 and click `Start swarming`
- Look in Cloud Console -> Trace -> Trace List and set HTTP Method to GET for filtering in application requests

## Test Istio fault injection

Apply Istio VirtualService manifests and check Delay and Errors in Kiali graph dashboard:

    kubectl apply -f istio-manifests/control/fault-abort.yaml

Delete Istio VirtualService manifests to revert to normal

    kubectl delete -f istio-manifests/control/fault-abort.yaml

Destroy tunnels for Locust, Kiali & Grafana: 

    tunnels/disconnect.sh


## Uninstall

**WARNING!!** This will delete everything installed so far.

To start uninstallation:

    ./teardown.sh

## Congratulations

<walkthrough-conclusion-trophy></walkthrough-conclusion-trophy>

You've just installed and tested out a very cool application completely on your own!
