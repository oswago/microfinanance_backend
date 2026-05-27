# Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy built JAR from builder stage
COPY --from=builder /app/target/microfinance-*.jar app.jar

# Create necessary directories
RUN mkdir -p /app/uploads /app/logs /app/data && \
    groupadd -r spring && useradd -r -g spring spring && \
    chown -R spring:spring /app

USER spring

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/api/management/health || exit 1

# Command to run the application with Docker profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]