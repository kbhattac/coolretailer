# CoolRetailer
### Autocomplete with Istio, Redis, BigQuery, Spring Boot, Spring Cloud and Stackdriver
- Dataset from BestBuy (converted to ndjson using JsonProcessor)
- Redis in Master/Slave replication mode (image from Bitnami)
- Autocomplete logic based on ZRANGEBYLEX Redis command
- Tunable cache size

### Installation:
- Get the Best Buy products dataset and save it to a GCS bucket: https://github.com/BestBuyAPIs/open-data-set/blob/master/products.json
- BQ requires the JSON to be new line delimited. Use the provided utility JSON processor for this:
```
java -jar ux-0.0.1-SNAPSHOT.jar --spring.profiles.active=JSON \ 
    --input.json=../src/main/resources/static/products.json
```
- Load it to BQ as dataset <code>coolretailer.products</code> from GCS
- Create a GCP service account with the following roles: 
<code>BigQuery Data Viewer, BigQuery Job User, Stackdriver Debugger Agent, Cloud Trace Agent, Context Graph Asserter, Error Reporting Admin, Logs Writer, Monitoring Metric Writer</code>
and install the private key as app-gac.json as kubernetes secret app-gac.
- set PROJECT_ID, TAG_NAME, REDIS_HOST in src/setup/kubernetes-manifests/coolretailer-app.yaml, coolretailer-ui.yaml


### Notes:
- Spring Cloud Sleuth Stackdriver Trace and Logging enabled.
- Complete CI/CD pipeline with Google Cloud Build.
- Test locally using:
```
$mvn spring-boot:run \
    -DGOOGLE_APPLICATION_CREDENTIALS=path-to-key-file \
    -DPROJECT_ID=coolretailer \
    -DGOOGLE_CLOUD_PROJECT=coolretailer \
    -Dspring-boot.run.arguments=--spring.redis.host=localhost
```
- Using docker from Cloud Shell:

(If tunneling to the redis service is required)
```
gcloud container clusters get-credentials coolretailer2 --zone europe-west4-a --project coolretailer \
 && kubectl port-forward $(kubectl get pod --selector="app=redis-master,role=master,tier=backend" --output jsonpath='{.items[0].metadata.name}') 6379:6379

gcloud auth configure-docker -q &&\
docker run \
    --network=host \
    -v /home/kbhattacharya:/etc/app-gac \
    -e GOOGLE_CLOUD_PROJECT=coolretailer \
    -e PROJECT_ID=coolretailer \
    -e GOOGLE_APPLICATION_CREDENTIALS=/etc/app-gac/app-gac.json \
    -e --spring.sleuth.sampler.probability=1.0 \
    -e --spring.application.name=ux-local \
    -e --spring.cloud.gcp.trace.enabled=true \
    -e --spring.cloud.gcp.logging.enabled=true \
    -e --spring.cloud.gcp.project-id=coolretailer \
    -it gcr.io/coolretailer/queryservice
```
- Use Cloud Memorystore instead of the Redis deployments (src/setup/kubernetes-manifests/archive/redis): https://cloud.google.com/memorystore/docs/redis/connect-redis-instance-gke
- Istio service mesh enabled.
- Istio TLS setup: https://istio.io/docs/tasks/traffic-management/secure-ingress/
- See setup/istio-manifests/secure for simple TLS configuration
- The static content is recommended to be served from a GCS bucket with Cloud CDN. Just drop off the content of src/ui/static/ui.* and src/ui/static/lib in a GCS bucket and provide allUsers READER access.
- Inspired by https://github.com/GoogleCloudPlatform/microservices-demo


Screenshots:
- Application:
![CoolRetailer](https://github.com/kbhattac/CoolRetailer/blob/master/capture.png)
- Stackdriver:
![Stackdriver](https://github.com/kbhattac/CoolRetailer/blob/master/stackdriver.png)
- Cloud Trace
![Trace](https://github.com/kbhattac/CoolRetailer/blob/master/trace.png)
