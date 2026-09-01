# First package the project
FROM maven:3-eclipse-temurin-25 AS build

WORKDIR /app
COPY . .
RUN --mount=type=secret,id=SENTRY_AUTH_TOKEN \
    SENTRY_AUTH_TOKEN=$(cat /run/secrets/SENTRY_AUTH_TOKEN 2>/dev/null) \
    mvn clean package -DskipTests -Dsentry.upload.skip=false

# Then run the packaged jar
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/src/main/resources/sentry.properties sentry.properties

# Version pinned by <sentry.version> in pom.xml; staged by maven-dependency-plugin.
COPY --from=build /app/target/agent/sentry-opentelemetry-agent.jar sentry-opentelemetry-agent.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
