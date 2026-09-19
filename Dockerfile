# ---------- Build stage: compile the Spring Boot jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# ---------- Run stage: small JRE-only image ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Don't run as root.
RUN useradd --system --create-home appuser
COPY --from=build /app/target/*.jar /app/app.jar
USER appuser

# Keep the JVM inside a small container's memory (Render Free/Starter = 512 MB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC"

# Render injects PORT (default 10000); application.yml already reads it (server.port: ${PORT:8080}).
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
