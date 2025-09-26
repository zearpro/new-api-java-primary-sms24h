#!/bin/bash

echo "🔍 Testing Database Connections..."
echo "=================================="

# MongoDB Connection Test
echo ""
echo "📊 Testing MongoDB Connection:"
echo "MongoDB URL: mongodb+srv://root:***@cluster0.ewvnv.mongodb.net/ativacoes"

# Using mongosh if available, otherwise use mongo
if command -v mongosh &> /dev/null; then
    MONGO_CMD="mongosh"
else
    MONGO_CMD="mongo"
fi

# Test MongoDB connection
timeout 10s $MONGO_CMD "mongodb+srv://root:6N5S0dASN1U62tNI@cluster0.ewvnv.mongodb.net/ativacoes?retryWrites=true&w=majority&appName=Cluster0" --eval "
try {
    db.runCommand({ping: 1});
    print('✅ MongoDB Connection: SUCCESS');
    print('📊 Database Stats:');
    print('   Database: ' + db.getName());
    print('   Collections: ' + db.getCollectionNames().length);
} catch(e) {
    print('❌ MongoDB Connection: FAILED');
    print('Error: ' + e.message);
}
" 2>/dev/null

if [ $? -ne 0 ]; then
    echo "❌ MongoDB Connection: FAILED (timeout or mongosh/mongo not available)"
    echo "   Testing with alternative method..."
    
    # Alternative test using curl (if available)
    if command -v curl &> /dev/null; then
        echo "   Trying DNS resolution for cluster0.ewvnv.mongodb.net..."
        if nslookup cluster0.ewvnv.mongodb.net > /dev/null 2>&1; then
            echo "   ✅ DNS Resolution: SUCCESS"
        else
            echo "   ❌ DNS Resolution: FAILED"
        fi
    fi
fi

echo ""
echo "🔧 Testing Redis Connection (Docker Container):"
echo "Redis Host: localhost:6379 (Container: store24h-redis)"

# Test Redis connection
if command -v redis-cli &> /dev/null; then
    timeout 5s redis-cli -h localhost -p 6379 -a store24h_redis_pass ping 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✅ Redis Connection: SUCCESS"
        # Get Redis info
        echo "   Redis Version: $(redis-cli -h localhost -p 6379 -a store24h_redis_pass info server | grep redis_version | cut -d: -f2 | tr -d '\r')"
        echo "   Redis Memory: $(redis-cli -h localhost -p 6379 -a store24h_redis_pass info memory | grep used_memory_human | cut -d: -f2 | tr -d '\r')"
    else
        echo "❌ Redis Connection: FAILED (container not running?)"
        echo "   Try: docker-compose up redis -d"
        echo "   Or: docker start store24h-redis"
    fi
else
    echo "⚠️  Redis CLI not available - skipping test"
    echo "   Install redis-tools: sudo apt-get install redis-tools"
    echo "   Docker test: docker exec store24h-redis redis-cli -a store24h_redis_pass ping"
fi

echo ""
echo "💾 Testing MySQL Connection:"
echo "MySQL Host: zdb.cluster-akn911wxcp.us.aws.sms24h.org:3306"

# Test MySQL connection
if command -v mysql &> /dev/null; then
    timeout 10s mysql -h zdb.cluster-akn911wxcp.us.aws.sms24h.org -P 3306 -u coredbuser -p78f77cfc780794aA#W -e "SELECT 1;" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✅ MySQL Connection: SUCCESS"
    else
        echo "❌ MySQL Connection: FAILED"
    fi
else
    echo "⚠️  MySQL client not available - skipping test"
fi

echo ""
echo "🚀 Quick Start Commands:"
echo "=================================="
echo "Local Development:"
echo "  docker-compose --env-file .env.local up -d"
echo ""
echo "Production Deployment (EC2):"
echo "  docker-compose up -d"
echo ""
echo "Redis Management:"
echo "  docker exec -it store24h-redis redis-cli -a store24h_redis_pass"
echo "  docker logs store24h-redis"
echo ""
echo "Test API endpoints:"
echo "  curl http://localhost:80/"
echo "  curl http://localhost:80/health"
echo "  curl http://localhost:80/actuator/health"
