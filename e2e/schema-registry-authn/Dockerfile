# Stage 1: Build the Maven application
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package

# Stage 2: Create a minimal runtime image
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/schema-registry-authn-1.0.0.jar .

EXPOSE 8080
CMD ["java", "-jar", "schema-registry-authn-1.0.0.jar"]