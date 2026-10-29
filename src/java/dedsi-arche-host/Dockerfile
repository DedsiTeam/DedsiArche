FROM eclipse-temurin:26-jre

WORKDIR /app

ARG JAR_FILE=target/dedsi-arche-host-1.0.0-SNAPSHOT.jar
COPY --chown=10001:10001 ${JAR_FILE} app.jar

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
