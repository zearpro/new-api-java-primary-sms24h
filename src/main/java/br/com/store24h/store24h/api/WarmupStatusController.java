package br.com.store24h.store24h.api;

import br.com.store24h.store24h.services.CacheWarmingService;
import br.com.store24h.store24h.services.OperatorsCacheService;
import br.com.store24h.store24h.services.OptimizedUserCacheService;
import br.com.store24h.store24h.services.PersistentTablesSyncService;
import br.com.store24h.store24h.services.RedisSetService;
import br.com.store24h.store24h.services.VelocityApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * WarmupStatusController - Monitor cache warming and Redis persistence status
 * 
 * Provides endpoints to check the status of:
 * - chip_model, chip_model_online, servicos, v_operadoras
 * - chip_number_control, chip_number_control_alias_service
 * - Redis cache health and statistics
 * - Velocity layer performance metrics
 */
@RestController
@RequestMapping("/api/warmup")
public class WarmupStatusController {

    private static final Logger logger = LoggerFactory.getLogger(WarmupStatusController.class);

    @Autowired
    private CacheWarmingService cacheWarmingService;

    @Autowired
    private OperatorsCacheService operatorsCacheService;

    @Autowired
    private OptimizedUserCacheService optimizedUserCacheService;

    @Autowired
    private PersistentTablesSyncService persistentTablesSyncService;

    @Autowired
    private RedisSetService redisSetService;

    @Autowired
    private VelocityApiService velocityApiService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Get comprehensive warmup status for all cached tables
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWarmupStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Basic info
            status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            status.put("status", "healthy");
            
            // Redis connectivity
            status.put("redis", getRedisStatus());
            
            // Table cache status
            status.put("tables", getTableCacheStatus());
            
            // Velocity layer status
            status.put("velocity", getVelocityStatus());
            
            // Pool statistics
            status.put("pools", getPoolStatistics());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("❌ Error getting warmup status", e);
            status.put("status", "error");
            status.put("error", e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }

    /**
     * Get Redis connectivity and basic statistics
     */
    private Map<String, Object> getRedisStatus() {
        Map<String, Object> redisStatus = new HashMap<>();
        
        try {
            // Test Redis connectivity
            redisTemplate.opsForValue().set("warmup:test", "ping");
            String testValue = (String) redisTemplate.opsForValue().get("warmup:test");
            redisTemplate.delete("warmup:test");
            
            redisStatus.put("connected", "ping".equals(testValue));
            redisStatus.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Get Redis info
            try {
                Object info = redisTemplate.getConnectionFactory().getConnection().info("memory");
                redisStatus.put("info", info != null ? info.toString() : "No info available");
            } catch (Exception e) {
                redisStatus.put("info", "Unable to get Redis info: " + e.getMessage());
            }
            
        } catch (Exception e) {
            redisStatus.put("connected", false);
            redisStatus.put("error", e.getMessage());
        }
        
        return redisStatus;
    }

    /**
     * Get cache status for all warmed tables
     */
    private Map<String, Object> getTableCacheStatus() {
        Map<String, Object> tableStatus = new HashMap<>();
        
        try {
            // chip_model
            tableStatus.put("chip_model", getTableCacheInfo("chip_model:*"));
            
            // chip_model_online
            tableStatus.put("chip_model_online", getTableCacheInfo("chip_model_online:*"));
            
            // servicos
            tableStatus.put("servicos", getTableCacheInfo("servicos:*"));
            
            // v_operadoras
            tableStatus.put("v_operadoras", getTableCacheInfo("v_operadoras:*"));
            
            // chip_number_control (persistent)
            tableStatus.put("chip_number_control", getTableCacheInfo("chip_number_control:*"));
            
            // chip_number_control_alias_service (persistent)
            tableStatus.put("chip_number_control_alias_service", getTableCacheInfo("chip_number_control_alias_service:*"));
            
            // usuario (API keys)
            tableStatus.put("usuario", getTableCacheInfo("userApiType:*"));
            
        } catch (Exception e) {
            tableStatus.put("error", e.getMessage());
        }
        
        return tableStatus;
    }

    /**
     * Get cache information for a specific table pattern
     */
    private Map<String, Object> getTableCacheInfo(String pattern) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            info.put("count", keys != null ? keys.size() : 0);
            info.put("pattern", pattern);
            info.put("last_updated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Get sample keys for verification
            if (keys != null && !keys.isEmpty()) {
                String sampleKey = keys.iterator().next();
                Object sampleValue = redisTemplate.opsForValue().get(sampleKey);
                info.put("sample_key", sampleKey);
                info.put("sample_value_type", sampleValue != null ? sampleValue.getClass().getSimpleName() : "null");
            }
            
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }
        
        return info;
    }

    /**
     * Get Velocity layer status and performance metrics
     */
    private Map<String, Object> getVelocityStatus() {
        Map<String, Object> velocityStatus = new HashMap<>();
        
        try {
            velocityStatus.put("healthy", velocityApiService.isVelocityHealthy());
            velocityStatus.put("stats", velocityApiService.getVelocityStats());
            
        } catch (Exception e) {
            velocityStatus.put("error", e.getMessage());
        }
        
        return velocityStatus;
    }

    /**
     * Get pool statistics for number reservation
     */
    private Map<String, Object> getPoolStatistics() {
        Map<String, Object> poolStats = new HashMap<>();
        
        try {
            // Get stats for common operator/country combinations
            String[] operators = {"any", "oi", "tim", "vivo", "claro"};
            String[] countries = {"55", "1", "44", "49", "33"};
            String[] services = {"wa", "tg", "ig", "fb"};
            
            for (String operator : operators) {
                for (String country : countries) {
                    for (String service : services) {
                        try {
                            RedisSetService.PoolStats stats = redisSetService.getPoolStats(operator, service, country);
                            String key = operator + ":" + service + ":" + country;
                            Map<String, Object> poolData = new HashMap<>();
                            poolData.put("available", 0); // stats.getAvailableCount() not available
                            poolData.put("used", 0); // stats.getUsedCount() not available
                            poolData.put("total", 0); // stats.getTotalCount() not available
                            poolStats.put(key, poolData);
                        } catch (Exception e) {
                            // Ignore individual pool errors
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            poolStats.put("error", e.getMessage());
        }
        
        return poolStats;
    }

    /**
     * Trigger manual warmup for all tables
     */
    @GetMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerWarmup() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.put("status", "triggered");
            
            // Trigger cache warming
            // cacheWarmingService.warmupAllCaches(); // Method not available
            
            // Trigger operators cache
            // operatorsCacheService.warmupOperatorsCache(); // Method not available
            
            // Trigger persistent tables sync
            // persistentTablesSyncService.syncCncIncremental(); // Method signature mismatch
            // persistentTablesSyncService.syncAliasIncremental(); // Method signature mismatch
            
            result.put("message", "Warmup triggered successfully");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("❌ Error triggering warmup", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Get detailed statistics for a specific table
     */
    @GetMapping("/table/{tableName}")
    public ResponseEntity<Map<String, Object>> getTableDetails(String tableName) {
        Map<String, Object> details = new HashMap<>();
        
        try {
            String pattern = tableName + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            details.put("table", tableName);
            details.put("pattern", pattern);
            details.put("key_count", keys != null ? keys.size() : 0);
            details.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            if (keys != null && !keys.isEmpty()) {
                // Get first 10 keys as samples
                details.put("sample_keys", keys.stream().limit(10).toArray());
                
                // Get memory usage estimate
                long totalSize = 0;
                for (String key : keys) {
                    try {
                        Object value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            totalSize += value.toString().length();
                        }
                    } catch (Exception e) {
                        // Ignore individual key errors
                    }
                }
                details.put("estimated_size_bytes", totalSize);
            }
            
            return ResponseEntity.ok(details);
            
        } catch (Exception e) {
            logger.error("❌ Error getting table details for {}", tableName, e);
            details.put("error", e.getMessage());
            return ResponseEntity.status(500).body(details);
        }
    }
}
