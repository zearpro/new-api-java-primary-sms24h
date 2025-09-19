# Store24h API - Production Deployment

SMS API service for Store24h platform with dynamic country code support.

## 🚀 Quick Deployment on EC2

### Prerequisites
- Docker and Docker Compose installed on EC2 instance
- External services configured (MySQL, MongoDB, Redis)
- Port 80 open in security group

### Deployment Steps

1. **Clone/Upload the project to your EC2 instance**

2. **Configure environment variables**
   ```bash
   cp .env .env.production
   # Edit .env.production with your production values
   nano .env.production
   ```

3. **Deploy the application**
   ```bash
   chmod +x deploy.sh
   ./deploy.sh
   ```

## 📋 Environment Variables

Update the following variables in `.env` file:

### Database Configuration
```bash
MYSQL_HOST=your-mysql-endpoint.region.rds.amazonaws.com
MYSQL_USER=coredbuser
MYSQL_PASSWORD=your-password
MYSQL_DATABASE=coredb

MONGO_URL=mongodb://your-mongo-endpoint:27017/ativacoes?connectTimeoutMS=300000&minPoolSize=100&maxPoolSize=450&maxIdleTimeMS=900000

REDIS_HOST=your-redis-endpoint.region.cache.amazonaws.com
REDIS_PORT=6379
```

### Application Configuration
```bash
LISTEN_PORT=80
```

## 🔧 Manual Docker Commands

Build the image:
```bash
docker-compose build
```

Start the application:
```bash
docker-compose up -d
```

Check logs:
```bash
docker-compose logs -f
```

Stop the application:
```bash
docker-compose down
```

## 🏥 Health Checks

- Health endpoint: `http://localhost:80/actuator/health`
- API documentation: `http://localhost:80/docs/`
- Application metrics: `http://localhost:80/actuator/metrics`

## 🔄 Key Updates Made

- **Removed hardcoded country code "73"** - Now accepts dynamic country parameters
- **Updated all API endpoints** to support multiple countries
- **Created production-ready Docker setup** with multi-stage build
- **Security improvements** with non-root user execution
- **Performance optimized** with G1 garbage collector

## 📁 Project Structure

```
.
├── src/                     # Java source code
├── lib/                     # Dependency JAR files
├── Dockerfile               # Production Docker configuration
├── docker-compose.yml       # Production compose file
├── .env                     # Environment variables template
├── deploy.sh               # Deployment script
└── README.md               # This file
```

## 🎯 API Endpoints

All endpoints now support dynamic country codes via request parameters:

- `GET /api/getNumber?country=XX` - Get phone number for country
- `GET /api/getPrices?country=XX` - Get pricing for country
- `POST /api/getExtraActivation` - Reactivate number (with country support)

## 🔒 Security Features

- Non-root container execution
- Health checks with timeout
- Resource limits and performance tuning
- Secrets via environment variables# new-api-java-primary-sms24h
