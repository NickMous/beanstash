# First package the project
FROM maven:3-eclipse-temurin-25 AS build

ARG SENTRY_AUTH_TOKEN
ENV SENTRY_AUTH_TOKEN=${SENTRY_AUTH_TOKEN}

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -Dsentry.upload.skip=false

# Then run the packaged jar
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ADD https://github.com/getsentry/sentry-java/releases/download/8.33.0/sentry-opentelemetry-agent-8.33.0.jar sentry-opentelemetry-agent.jar

EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:sentry-opentelemetry-agent.jar", "-jar", "app.jar"]
