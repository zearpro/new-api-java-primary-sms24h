# 🚀 Quick Deployment Guide - Dragonfly Performance Edition

## 📋 **Files You Need to Upload Manually**

### **1. Core Application Files**
```
✅ src/main/resources/application.properties  (Dragonfly config)
✅ docker-compose.yml                          (Updated with Dragonfly + Dashboard)
✅ Dockerfile                                  (Java API container)
✅ pom.xml                                     (Maven dependencies)
```

### **2. Environment Files**
```
✅ .env                                        (Production environment)
✅ .env.dev                                    (Development environment)  
✅ .env.example                                (Template with all settings)
```

### **3. Dashboard Files (NEW)**
```
✅ dashboard/
   ├── package.json                            (React dependencies)
   ├── Dockerfile                              (Dashboard container)
   ├── tailwind.config.js                      (Tailwind configuration)
   ├── postcss.config.js                       (PostCSS configuration)
   ├── public/index.html                       (HTML template)
   └── src/
       ├── index.js                            (React entry point)
       ├── index.css                           (Tailwind styles)
       ├── App.js                              (Main dashboard app)
       └── components/
           ├── MetricsOverview.jsx             (System metrics)
           ├── WarmupStatus.jsx                (Cache warming status)
           ├── RedisHealth.jsx                 (Dragonfly health)
           ├── TableProgress.jsx                (Table loading progress)
           ├── ControlPanel.jsx                 (Manual controls)
           └── PerformanceCharts.jsx            (Live charts)
```

### **4. Deployment Scripts**
```
✅ dev.sh                                      (Development deployment)
✅ deploy.sh                                   (Production deployment)
```

### **5. Documentation**
```
✅ README.md                                   (Complete documentation)
```

## 🚀 **Deployment Commands**

### **Development Environment**
```bash
# Make scripts executable
chmod +x dev.sh deploy.sh

# Start development environment
./dev.sh
```

### **Production Environment**
```bash
# Deploy to production
./deploy.sh
```

## 📊 **Service URLs After Deployment**

| Service | URL | Description |
|---------|-----|-------------|
| **Main API** | http://your-server:80 | Store24h API |
| **Dashboard** | http://your-server:3000 | Beautiful monitoring UI |
| **Accelerator** | http://your-server:3001 | Hono.js microservice |
| **RabbitMQ** | http://your-server:15672 | Message queue management |

## 🔧 **Manual Deployment Steps**

### **1. Upload Files**
```bash
# Upload all files to your EC2 instance
scp -r . user@your-server:/path/to/api-ecr-extracted/
```

### **2. Configure Environment**
```bash
# Edit production environment
nano .env

# Update database credentials
MYSQL_HOST=your-mysql-host
MYSQL_USER=your-mysql-user
MYSQL_PASSWORD=your-mysql-password
MYSQL_DATABASE=your-mysql-database
```

### **3. Deploy**
```bash
# Make scripts executable
chmod +x dev.sh deploy.sh

# Deploy to production
./deploy.sh
```

### **4. Verify Deployment**
```bash
# Check container status
docker-compose ps

# Check logs
docker-compose logs -f

# Test endpoints
curl http://localhost/api/warmup/status
curl http://localhost:3000
```

## 🎯 **Expected Results**

After deployment, you should see:

- ✅ **DragonflyDB** running (3.8x faster than Redis)
- ✅ **Beautiful Dashboard** at port 3000
- ✅ **Fast Cache Warming** (2-5 minutes vs 15+ minutes)
- ✅ **Real-time Monitoring** with live charts
- ✅ **Manual Control Panel** for cache management

## 🔍 **Troubleshooting**

### **Dashboard Not Loading**
```bash
# Check dashboard logs
docker-compose logs dashboard

# Rebuild dashboard
docker-compose build --no-cache dashboard
docker-compose up -d dashboard
```

### **Dragonfly Connection Issues**
```bash
# Check Dragonfly logs
docker-compose logs dragonfly

# Test Dragonfly connection
docker exec store24h-dragonfly redis-cli ping
```

### **Cache Warming Issues**
```bash
# Check API logs
docker-compose logs store24h-api

# Manual trigger via API
curl -X POST http://localhost/api/warmup/trigger
```

## 📞 **Support**

- **Dashboard**: http://your-server:3000
- **API Health**: http://your-server/actuator/health
- **Warmup Status**: http://your-server/api/warmup/status
- **Logs**: `docker-compose logs -f`

---

**🎉 Your Dragonfly Performance Edition is ready for high-performance SMS operations!**
