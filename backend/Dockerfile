# Multi-stage build for Spring Boot application
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src src

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR file from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Copy uploads directory for pet sprites
RUN mkdir -p /app/uploads/sprites && chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]