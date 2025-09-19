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
RUN mvn clean package -DskipTests

# Stage 2: Create the final runtime image
FROM eclipse-temurin:17-jre

# Install curl and other utilities for health checks and debugging
RUN apt-get update && \
    apt-get install -y curl wget netcat-traditional && \
    rm -rf /var/lib/apt/lists/*

# Create app user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /app/target/store24h-1.0.0.jar ./app.jar

# Copy the application properties from the builder stage
COPY --from=builder /app/target/classes/application.properties ./application.properties

# Create a startup script for better error handling
RUN echo '#!/bin/bash' > startup.sh && \
    echo 'set -e' >> startup.sh && \
    echo '' >> startup.sh && \
    echo 'echo "=== Store24h API Starting (Original JAR) ==="' >> startup.sh && \
    echo 'echo "Java Version: $(java -version 2>&1 | head -1)"' >> startup.sh && \
    echo 'echo "JAR Size: $(ls -lh app.jar | awk '"'"'{print $5}'"'"')"' >> startup.sh && \
    echo 'echo "Environment:"' >> startup.sh && \
    echo 'echo "  - Database: ${MYSQL_HOST:-not_set}"' >> startup.sh && \
    echo 'echo "  - Redis: ${REDIS_HOST:-not_set}"' >> startup.sh && \
    echo 'echo "  - Port: ${LISTEN_PORT:-80}"' >> startup.sh && \
    echo 'echo "=========================="' >> startup.sh && \
    echo '' >> startup.sh && \
    echo '# Check JAR contents' >> startup.sh && \
    echo 'echo "JAR Contents Check:"' >> startup.sh && \
    echo 'jar -tf app.jar | grep -E "Store24hApplication|MANIFEST" | head -5 || echo "No main class pattern found"' >> startup.sh && \
    echo '' >> startup.sh && \
    echo '# Start the application with detailed logging' >> startup.sh && \
    echo 'exec java \' >> startup.sh && \
    echo '    -Xms512m \' >> startup.sh && \
    echo '    -Xmx2g \' >> startup.sh && \
    echo '    -XX:+UseG1GC \' >> startup.sh && \
    echo '    -XX:G1HeapRegionSize=16m \' >> startup.sh && \
    echo '    -XX:+UseStringDeduplication \' >> startup.sh && \
    echo '    -Djava.awt.headless=true \' >> startup.sh && \
    echo '    -Dspring.config.location=classpath:/application.properties,file:./application.properties \' >> startup.sh && \
    echo '    -Dlogging.level.root=INFO \' >> startup.sh && \
    echo '    -Dspring.main.banner-mode=console \' >> startup.sh && \
    echo '    -jar app.jar "$@"' >> startup.sh

RUN chmod +x startup.sh

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to a non-root user for security
USER appuser

# Set the entrypoint to our startup script
ENTRYPOINT ["./startup.sh"]

EXPOSE 80

# Health check for EC2 deployment
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${LISTEN_PORT:-80}/actuator/health || exit 1