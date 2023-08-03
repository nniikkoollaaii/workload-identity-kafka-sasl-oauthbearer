FROM eclipse-temurin:17-jre

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file from the target folder of the host to the working directory in the container
COPY target/gateway-application.jar /app/

# Expose the port that the Spring Cloud Gateway application listens on
EXPOSE 8080

# Command to run the Spring Cloud Gateway application when the container starts
CMD ["java", "-jar", "gateway-application.jar"]
