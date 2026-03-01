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

ADD https://repo1.maven.org/maven2/io/sentry/sentry-opentelemetry-agent/8.33.0/sentry-opentelemetry-agent-8.33.0.jar sentry-opentelemetry-agent.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
