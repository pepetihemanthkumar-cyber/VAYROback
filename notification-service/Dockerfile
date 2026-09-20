# Multi-stage Dockerfile for VAYRO Notification Service

# ==============================================================================
# Stage 1: Build & Package with Maven + Java 21
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy root reactor POM and install parent POM locally into repository
COPY pom.xml ./pom.xml
RUN mvn -N install -DskipTests -B

# Copy notification-service module POM and source code
COPY notification-service/pom.xml ./notification-service/pom.xml
COPY notification-service/src ./notification-service/src

# Package production-ready Spring Boot executable JAR
RUN mvn clean package -pl notification-service -DskipTests -B

# ==============================================================================
# Stage 2: Minimal Java 21 Production Runtime
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create unprivileged application user and group for container security
RUN addgroup -S vayro && adduser -S vayro -G vayro

# Copy built JAR from builder stage
COPY --from=builder /build/notification-service/target/notification-service-*.jar /app/app.jar

# Set directory permissions
RUN chown -R vayro:vayro /app

USER vayro

# Render dynamically injects the PORT environment variable
ENV SERVER_PORT=8084
EXPOSE 8084

# Entrypoint supporting Render PORT mapping, urandom seed, and optimal memory management
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8084} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
