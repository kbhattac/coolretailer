# app
FROM gcr.io/distroless/java
COPY target/dataservice-1.0.0.jar /app.jar
EXPOSE 8080/tcp
EXPOSE 6565/tcp
ENTRYPOINT ["java", "-jar", "/app.jar"]