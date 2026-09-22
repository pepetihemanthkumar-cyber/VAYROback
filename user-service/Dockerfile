# Multi-stage Dockerfile for VAYRO User Service

# ==============================================================================
# Stage 1: Build & Package with Maven + Java 21
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy root parent POM (used for dependencyManagement and properties)
COPY pom.xml ./pom.xml

# Copy only user-service module POM and source code
COPY user-service/pom.xml ./user-service/pom.xml
COPY user-service/src ./user-service/src

# Build user-service directly without invoking sibling modules
RUN mvn -f user-service/pom.xml clean package -DskipTests -B

# ==============================================================================
# Stage 2: Minimal Java 21 Production Runtime
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create unprivileged application user and group for container security
RUN addgroup -S vayro && adduser -S vayro -G vayro

# Copy built JAR from builder stage
COPY --from=builder /build/user-service/target/user-service-*.jar /app/app.jar

# Set directory permissions
RUN chown -R vayro:vayro /app

USER vayro

# Render dynamically injects the PORT environment variable
ENV SERVER_PORT=8081
EXPOSE 8081

# Entrypoint supporting Render PORT mapping, urandom seed, and optimal memory management
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8081} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
