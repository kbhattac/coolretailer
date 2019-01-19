# app
FROM gcr.io/distroless/java
COPY target/queryservice-1.0.0.jar /app.jar
EXPOSE 8080/tcp
ENTRYPOINT ["java","-jar", "/app.jar"]