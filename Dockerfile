# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Copy Maven wrapper and pom first to leverage Docker layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Pre-fetch dependencies (speeds up subsequent builds)
RUN chmod +x mvnw \
    && ./mvnw -B -q -DskipTests dependency:go-offline

# Copy source and build the application
COPY src ./src
RUN ./mvnw -B -DskipTests package \
    && cp target/*.jar /app/app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Expose the application port (configured in application.yml)
EXPOSE 8085

# Allow passing extra JVM options at runtime
ENV JAVA_OPTS=""

# Copy the built artifact
COPY --from=builder /app/app.jar /app/app.jar

# Use sh -c to enable JAVA_OPTS expansion
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
