# External Services Configuration

This project has been configured to connect to external services (MySQL, MongoDB, Redis) instead of running them locally.

## Quick Setup

1. **Copy the environment template:**
   ```bash
   cp .env.example .env
   ```

2. **Update `.env` with your external service details:**
   - MySQL: Update `MYSQL_HOST`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_DATABASE`
   - MongoDB: Update `MONGO_URL` with your external MongoDB connection string
   - Redis: Update `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`

3. **Run the application:**
   ```bash
   docker-compose up -d
   ```

## Configuration Details

### MySQL Database
The application is configured to connect to an external MySQL database. Update these environment variables:

```env
MYSQL_HOST=your-external-mysql-host.amazonaws.com
MYSQL_PORT=3306
MYSQL_USER=your-mysql-username
MYSQL_PASSWORD=your-mysql-password
MYSQL_DATABASE=your-database-name
```

### MongoDB
For external MongoDB, update the full connection string:

```env
MONGO_URL=mongodb://username:password@your-mongo-host:27017/database-name?options
```

### Redis
For external Redis configuration:

```env
REDIS_HOST=your-external-redis-host.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
```

## Running the Application

### Using Docker Compose (Recommended)
```bash
# Make sure .env file is configured
docker-compose up -d
```

### Using Docker Directly
```bash
# Build the image
docker build -t store24h-api .

# Run with environment variables
docker run -d \
  --name store24h-api \
  -p 8080:8080 \
  --env-file .env \
  store24h-api
```

### Local Development
```bash
# Set environment variables in your IDE or export them
export MYSQL_HOST=your-external-mysql-host
export MONGO_URL=mongodb://your-external-mongo-host:27017/ativacoes
export REDIS_HOST=your-external-redis-host

# Run with Maven
mvn spring-boot:run
```

## Service Requirements

### MySQL
- Version: 8.0 or higher
- Required permissions: CREATE, SELECT, INSERT, UPDATE, DELETE
- Connection pooling is configured with HikariCP

### MongoDB
- Version: 4.4 or higher
- Database: `ativacoes`
- Auto-indexing is enabled

### Redis
- Version: 6.0 or higher
- Used for caching and session storage
- No special configuration required

## Troubleshooting

### Connection Issues
1. Verify network connectivity to external services
2. Check firewall rules and security groups
3. Validate credentials and connection strings
4. Review application logs for specific error messages

### Performance Tuning
The application includes optimized connection pool settings. Adjust these environment variables if needed:

```env
HIKARI_MINIMUM_IDLE=50
TOMCAT_MAX_THREADS=1000
ASYNC_EXECUTOR_CORE_POOL_SIZE=1000
```

## Security Notes

- Never commit the `.env` file to version control
- Use secure connections (SSL/TLS) for production databases
- Rotate passwords regularly
- Use IAM roles and proper authentication when possible
