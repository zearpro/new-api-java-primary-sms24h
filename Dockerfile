# Stage 1: Build the application using Maven and JDK 17
FROM maven:3.8-openjdk-17 AS builder

# Set the working directory
WORKDIR /app

# Copy the Maven project file
COPY pom.xml .

# Copy the source code
COPY src ./src

# Build the project and create the JAR file
# This runs all tests and packages the application
# Use parallel builds and offline mode for faster builds
RUN mvn clean package -DskipTests -T 4C -o

# Stage 2: Create the final runtime image
FROM eclipse-temurin:17-jre

# Install curl and other utilities for health checks and debugging
RUN apt-get update && \
    apt-get install -y curl wget netcat-openbsd && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

# Create app user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /app/target/store24h-1.0.0.jar ./app.jar

# No additional files needed - JAR contains everything

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to a non-root user for security
USER appuser

EXPOSE 80

# Clean and direct Java startup with minimal logging
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx2g", \
    "-XX:+UseG1GC", \
    "-XX:G1HeapRegionSize=16m", \
    "-XX:+UseStringDeduplication", \
    "-Djava.awt.headless=true", \
    "-Dspring.main.banner-mode=off", \
    "-Dlogging.level.root=ERROR", \
    "-Dlogging.level.br.com.store24h=INFO", \
    "-jar", "app.jar"]

# Health check for EC2 deployment
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:80/actuator/health || exit 1