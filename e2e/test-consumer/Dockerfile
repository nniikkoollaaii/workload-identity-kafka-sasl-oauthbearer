# Use an appropriate base image with Java support (e.g., AdoptOpenJDK, OpenJDK, etc.)
FROM adoptopenjdk:8-jre-hotspot

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file into the container
COPY target/kafka-consumer-app-1.0.0-jar-with-dependencies.jar /app/kafka-e2e/test-producer-app.jar

# Set the entrypoint command to run the JAR file
ENTRYPOINT ["java", "-jar", "kafka-e2e/test-producer-app.jar"]
