# To tell Docker to start with a Java 21 runtime
FROM eclipse-temurin:21-jre-jammy

# Creates and switches to the /app directory inside the container.
WORKDIR /app
COPY target/hands_on-0.0.1.jar /app/app.jar

# Documents that your Java server runs on port 8080.
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]
