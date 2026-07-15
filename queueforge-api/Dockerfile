# Build stage: compile the reactor and package the API jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY queueforge-common queueforge-common
COPY queueforge-api queueforge-api
COPY queueforge-worker queueforge-worker
RUN mvn -q -B clean package -DskipTests

# Run stage: slim JRE image with just the API jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/queueforge-api/target/queueforge-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
