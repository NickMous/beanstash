# First package the project
FROM maven:3-eclipse-temurin-25 AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Then run the packaged jar
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
