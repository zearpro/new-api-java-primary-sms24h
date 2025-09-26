# 🔄 Redis Container Setup - Local & EC2 Production

## 📋 **CHANGES IMPLEMENTED**

✅ **Redis is now fully containerized for ALL environments**
- ❌ **NO MORE External AWS MemoryDB** 
- ✅ **Redis Container** runs on same server (local & EC2)
- ✅ **Unified configuration** across all environments
- ✅ **Cost savings** - No external Redis service fees

## 🏗️ **Architecture Overview**

```
┌─────────────────────────────────────────┐
│             EC2 Server / Local          │
├─────────────────────────────────────────┤
│  ┌─────────────────┐ ┌─────────────────┐ │
│  │   Spring Boot   │ │   Redis 7       │ │
│  │   Application   │ │   Container     │ │
│  │   (Port 80)     │ │   (Port 6379)   │ │
│  └─────────────────┘ └─────────────────┘ │
├─────────────────────────────────────────┤
│           Docker Network                │
├─────────────────────────────────────────┤
│  External: MongoDB Atlas + MySQL RDS   │
└─────────────────────────────────────────┘
```

## ⚙️ **Configuration Changes**

### **1. Docker Compose - Production Ready Redis**
```yaml
redis:
  image: redis:7-alpine
  container_name: store24h-redis
  ports:
    - "6379:6379"
  command: >
    redis-server 
    --appendonly yes 
    --requirepass store24h_redis_pass
    --maxmemory 512mb
    --maxmemory-policy allkeys-lru
    --save 900 1
    --save 300 10
    --save 60 10000
  volumes:
    - redis_data:/data
  restart: unless-stopped
```

### **2. Environment Variables (ALL .env files)**
```bash
# Redis Configuration (Container - All Environments)
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=store24h_redis_pass
USE_REDIS_REMOTE=false
```

### **3. Redis Configuration (Java)**
- ✅ **Simplified RedisConfig.java** - No more cluster configuration
- ✅ **Standard connection** to Redis container
- ✅ **Password authentication**
- ✅ **Connection validation**

## 🚀 **Deployment Instructions**

### **For EC2 Production:**

**1. Upload files to EC2:**
```bash
scp -i your-key.pem docker-compose.yml ec2-user@your-ec2-ip:/home/ec2-user/
scp -i your-key.pem .env ec2-user@your-ec2-ip:/home/ec2-user/
```

**2. On EC2 server:**
```bash
# Install Docker & Docker Compose (if not already installed)
sudo yum update -y
sudo yum install -y docker
sudo usermod -a -G docker ec2-user
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Deploy the application
docker-compose up -d
```

**3. Verify deployment:**
```bash
# Check containers
docker ps

# Check Redis
docker exec -it store24h-redis redis-cli -a store24h_redis_pass ping

# Check application
curl http://localhost:80/health
```

### **For Local Development:**
```bash
# Same command works for local development
docker-compose --env-file .env.local up -d

# Or use the main .env file
docker-compose up -d
```

## 🔒 **Security Configuration**

### **Redis Security Features:**
- ✅ **Password protection** - `requirepass store24h_redis_pass`
- ✅ **Container network isolation** - Not exposed to internet
- ✅ **Data persistence** - AOF + RDB snapshots
- ✅ **Memory limits** - 512MB max memory with LRU eviction

### **Production Security Recommendations:**
```bash
# Change default password in .env
REDIS_PASSWORD=your_secure_password_here

# Ensure EC2 security group only allows:
# - Port 80 (HTTP) from 0.0.0.0/0
# - Port 22 (SSH) from your IP only
# - Port 6379 (Redis) should NOT be exposed to internet
```

## 📊 **Performance Configuration**

### **Redis Optimizations:**
- **Memory Policy:** `allkeys-lru` - Evict least recently used keys
- **Persistence:** AOF + RDB for data durability
- **Memory Limit:** 512MB (adjust based on EC2 instance size)
- **Connection Pool:** Lettuce with validation

### **Scaling Recommendations:**

| EC2 Instance | Redis Memory | Expected Load |
|--------------|--------------|---------------|
| **t3.micro** | 256MB | Light development |
| **t3.small** | 512MB | Production (current) |
| **t3.medium** | 1GB | High traffic |
| **t3.large** | 2GB | Very high traffic |

## 🔍 **Monitoring & Troubleshooting**

### **Redis Health Checks:**
```bash
# Check Redis status
docker exec store24h-redis redis-cli -a store24h_redis_pass info replication

# Monitor Redis memory usage
docker exec store24h-redis redis-cli -a store24h_redis_pass info memory

# Check Redis logs
docker logs store24h-redis
```

### **Application Health:**
```bash
# Check application connection to Redis
curl http://localhost:80/actuator/health

# Check application logs
docker logs store24h-api
```

### **Common Issues & Solutions:**

| Issue | Solution |
|-------|----------|
| **Redis connection failed** | `docker restart store24h-redis` |
| **Out of memory** | Increase `--maxmemory` in docker-compose.yml |
| **Data loss** | Check volume mount: `docker volume ls` |
| **Port conflicts** | Change port mapping in docker-compose.yml |

## 💰 **Cost Benefits**

### **Before (AWS MemoryDB):**
- Monthly cost: ~$50-200+ depending on usage
- External dependency
- Network latency

### **After (Container Redis):**
- Monthly cost: $0 (included in EC2 cost)
- Local to application
- No network latency

## 🎯 **Next Steps**

1. **Deploy to EC2** using the instructions above
2. **Test all endpoints** to ensure Redis connectivity
3. **Monitor performance** using the health checks
4. **Backup Redis data** if needed:
   ```bash
   docker exec store24h-redis redis-cli -a store24h_redis_pass BGSAVE
   ```

## 📱 **Quick Commands Reference**

```bash
# Start everything
docker-compose up -d

# Check Redis
docker exec -it store24h-redis redis-cli -a store24h_redis_pass ping

# Restart Redis only
docker-compose restart redis

# View Redis data
docker exec -it store24h-redis redis-cli -a store24h_redis_pass keys "*"

# Check application health
curl http://localhost:80/actuator/health
```

**✅ Redis is now fully self-contained and will run on the same EC2 server as your application!**
