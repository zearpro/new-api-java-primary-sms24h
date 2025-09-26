# 🚀 Store24h API - Local Development Setup

## ✅ **COMPLETED CHANGES**

### 1. **🔧 Dockerfile Optimization**
- ✅ **Removed verbose startup script** - No more complex shell script with debugging info
- ✅ **Direct Java execution** - Clean ENTRYPOINT with optimized JVM parameters
- ✅ **Reduced image complexity** - Simpler, faster container startup

### 2. **🔄 Redis Local Setup**
- ✅ **Added Redis container** to docker-compose.yml with Redis 7 Alpine
- ✅ **Persistent storage** with named volume `redis_data`
- ✅ **Health checks** for Redis service
- ✅ **Password protection** with configurable password

### 3. **📋 Environment Configuration**
- ✅ **`.env`** - Production configuration (AWS MemoryDB Redis)
- ✅ **`.env.local`** - Local development (Docker Redis)
- ✅ **`.env.production`** - Backup production configuration
- ✅ **`application-docker.properties`** - Docker-specific Spring Boot profile

### 4. **🔗 Connection Management**
- ✅ **Flexible Redis configuration** - Supports both local and external Redis
- ✅ **Container networking** - Redis accessible via service name `redis`
- ✅ **Dependency management** - API waits for Redis to be healthy

### 5. **🔇 Minimal Logging**
- ✅ **Ultra-quiet startup** - No more verbose framework logs
- ✅ **Error visibility** - Critical errors clearly displayed  
- ✅ **Clean container logs** - Only essential information
- ✅ **Custom log patterns** - Optimized for production monitoring

## 🚀 **HOW TO USE**

### **Local Development:**
```bash
# Start with local Redis container
docker-compose --env-file .env.local up

# Or just Redis service
docker-compose --env-file .env.local up redis
```

### **Production Deployment:**
```bash
# Use external AWS services
docker-compose up
```

### **Test Connections:**
```bash
# Make script executable
chmod +x test-connections.sh

# Run connection tests
./test-connections.sh
```

## 📊 **Service Configuration**

| Environment | Redis | MongoDB | MySQL |
|------------|-------|---------|-------|
| **Local** | Docker Container (localhost:6379) | External Atlas | External AWS RDS |
| **Production** | AWS MemoryDB | External Atlas | External AWS RDS |

## 🔍 **Connection Details**

### **Redis (Local Development)**
- **Host:** `redis` (container) / `localhost` (direct)
- **Port:** 6379
- **Password:** `store24h_redis_pass`
- **Storage:** Persistent with Redis AOF

### **MongoDB (External - Working)**
- **URL:** `mongodb+srv://cluster0.ewvnv.mongodb.net`
- **Database:** `ativacoes`
- **Status:** ✅ External connection maintained

### **MySQL (External - Working)**
- **Host:** `zdb.cluster-akn911wxcp.us.aws.sms24h.org`
- **Database:** `coredb`
- **Status:** ✅ External connection maintained

## 🔧 **Available Endpoints**

After starting the application:

- **🏠 Root:** `http://localhost:80/`
- **❤️ Health:** `http://localhost:80/health`
- **🔍 Actuator Health:** `http://localhost:80/actuator/health`
- **📚 API Docs:** `http://localhost:80/docs/`
- **📋 OpenAPI:** `http://localhost:80/api-docs`

## ⚡ **Performance Optimizations**

### **JVM Parameters (Applied in Dockerfile):**
- `-Xms512m -Xmx2g` - Memory allocation
- `-XX:+UseG1GC` - G1 Garbage Collector
- `-XX:G1HeapRegionSize=16m` - G1 heap region optimization
- `-XX:+UseStringDeduplication` - Memory optimization

### **Local vs Production Settings:**
- **Local:** Reduced thread pools for development
- **Production:** High-performance threading configuration

## 🔍 **Troubleshooting**

### **Redis Connection Issues:**
```bash
# Check if Redis container is running
docker ps | grep redis

# Test Redis connection
redis-cli -h localhost -p 6379 -a store24h_redis_pass ping
```

### **Application Won't Start:**
```bash
# Check logs
docker-compose --env-file .env.local logs store24h-api

# Check if port 80 is available
sudo lsof -i :80
```

### **MongoDB Connection Test:**
The application uses MongoDB Atlas which should work from any internet connection. MongoDB connection is automatically tested during application startup.

## 🎯 **Next Steps**

1. **Start local development:** `docker-compose --env-file .env.local up`
2. **Test all endpoints** using the URLs above
3. **Monitor logs** for any connection issues
4. **Deploy to production** using standard `docker-compose up`
