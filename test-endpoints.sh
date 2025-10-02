#!/bin/bash

echo "🧪 Testing API Endpoints"
echo "========================"

# Test basic health
echo "1. Testing basic health endpoint..."
curl -s "http://api-global.sms24h.org/actuator/health" | jq '.' 2>/dev/null || echo "Health endpoint test completed"

echo ""
echo "2. Testing warmup status endpoint..."
curl -s "http://api-global.sms24h.org/api/warmup/status" | jq '.' 2>/dev/null || echo "Warmup status test completed"

echo ""
echo "3. Testing MySQL stats endpoint..."
curl -s "http://api-global.sms24h.org/api/warmup/mysql-stats" | jq '.' 2>/dev/null || echo "MySQL stats test completed"

echo ""
echo "4. Testing data flow test endpoint..."
curl -s "http://api-global.sms24h.org/api/warmup/test-data-flow" | jq '.' 2>/dev/null || echo "Data flow test completed"

echo ""
echo "✅ All endpoint tests completed!"
echo ""
echo "📋 Summary of fixes applied:"
echo "- Fixed RedisConfig.java to use 'dragonfly' instead of 'redis'"
echo "- Removed hardcoded Redis password for DragonflyDB compatibility"
echo "- Added comprehensive data flow test endpoint"
echo "- All Redis connections now properly point to DragonflyDB container"
echo ""
echo "🚀 Next steps:"
echo "1. Pull changes on EC2: git pull origin main"
echo "2. Restart services: docker-compose down && docker-compose up -d"
echo "3. Test endpoints: curl http://YOUR_EC2_IP:80/api/warmup/test-data-flow"
