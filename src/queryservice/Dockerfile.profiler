# app
FROM openjdk:8
RUN apt-get update
RUN mkdir -p /opt/cprof && \
    wget -q -O- https://storage.googleapis.com/cloud-profiler/java/latest/profiler_java_agent.tar.gz \
    | tar xzv -C /opt/cprof
COPY target/queryservice-1.0.0.jar /app.jar
EXPOSE 8080/tcp
ENTRYPOINT ["java", "-agentpath:/opt/cprof/profiler_java_agent.so=-cprof_service=queryservice,-cprof_project_id=coolretailer,-logtostderr","-jar", "/app.jar"]