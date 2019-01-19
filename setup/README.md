## CoolRetailer 
### Install
- Make sure `$PROJECT_ID` is set: `gcloud config set project $PROJECT_ID`
- Set application version: `VERSION` in `properties` if required (default is 1.0.0)
- To start installation: `./setup.sh`
- Check for the endpoint address after deployment finishes (can take 10-15 mins)
- Setup tunnels: `tunnels/connect.sh` for [Locust load generator(8080)](https://ssh.cloud.google.com/devshell/proxy?authuser=0&port=8080), [Kiali Dashboard(8081)](https://ssh.cloud.google.com/devshell/proxy?authuser=0&port=8081) & [Grafana Dashboard(8082)](https://ssh.cloud.google.com/devshell/proxy?authuser=0&port=8082)
- In Locust dashboard enter `Number of users to simulate = 10` and `hatch rate = 5` and click `Start swarming`
- Look in Cloud Console -> Trace -> Trace List and set HTTP Method to GET for filtering in application requests
- Apply Istio VirtualService manifests `kubectl apply -f istio-manifests/control/fault-abort.yaml`
- Check delay and errors in Kiali dashboard
- Delete Istio VirtualService manifests `kubectl delete -f istio-manifests/control/fault-abort.yaml`
- Destroy tunnels for Locust, Kiali & Grafana: `tunnels/disconnect.sh`
### Uninstall
- Make sure `$PROJECT_ID` is set: `gcloud config set project $PROJECT_ID`
- **WARNING!!** This will delete everything installed during the Install step above
- To start uninstallation: `./teardown.sh`