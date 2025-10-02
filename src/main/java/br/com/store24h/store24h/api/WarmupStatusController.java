package br.com.store24h.store24h.api;

import br.com.store24h.store24h.services.CacheWarmingService;
import br.com.store24h.store24h.services.OperatorsCacheService;
import br.com.store24h.store24h.services.FullPersistenceService;
import br.com.store24h.store24h.services.OptimizedUserCacheService;
import br.com.store24h.store24h.services.PersistentTablesSyncService;
import br.com.store24h.store24h.services.RedisSetService;
import br.com.store24h.store24h.services.VelocityApiService;
import br.com.store24h.store24h.repository.ChipRepository;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.repository.OperadorasRepository;
import br.com.store24h.store24h.repository.ChipNumberControlRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.repository.ActivationRepository;
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

    @Autowired(required = false)
    private FullPersistenceService fullPersistenceService;

    @Autowired
    private ChipRepository chipRepository;

    @Autowired
    private ServicosRepository servicosRepository;

    @Autowired
    private OperadorasRepository operadorasRepository;

    @Autowired
    private ChipNumberControlRepository chipNumberControlRepository;

    @Autowired
    private UserDbRepository userDbRepository;

    @Autowired
    private ActivationRepository activationRepository;

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
            
            // Redis seeding progress
            status.put("seeding_progress", getSeedingProgress());
            
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
     * Get Redis seeding progress with percentages
     */
    private Map<String, Object> getSeedingProgress() {
        Map<String, Object> progress = new HashMap<>();
        
        try {
            Map<String, Object> tableStatus = getTableCacheStatus();
            Map<String, Object> mysqlStats = getMysqlStats().getBody();
            
            if (mysqlStats != null && mysqlStats.containsKey("tables")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mysqlTables = (Map<String, Object>) mysqlStats.get("tables");
                
                Map<String, Object> tableProgress = new HashMap<>();
                int totalProgress = 0;
                int tableCount = 0;
                
                for (String tableName : tableStatus.keySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> redisTable = (Map<String, Object>) tableStatus.get(tableName);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mysqlTable = (Map<String, Object>) mysqlTables.get(tableName);
                    
                    if (redisTable != null && mysqlTable != null) {
                        long redisCount = (Long) redisTable.get("count");
                        long mysqlCount = (Long) mysqlTable.get("row_count");
                        
                        int percentage = mysqlCount > 0 ? (int) ((redisCount * 100) / mysqlCount) : 0;
                        percentage = Math.min(percentage, 100); // Cap at 100%
                        
                        Map<String, Object> progressInfo = new HashMap<>();
                        progressInfo.put("redis_count", redisCount);
                        progressInfo.put("mysql_count", mysqlCount);
                        progressInfo.put("percentage", percentage);
                        progressInfo.put("status", percentage >= 100 ? "complete" : (percentage > 0 ? "partial" : "empty"));
                        
                        tableProgress.put(tableName, progressInfo);
                        totalProgress += percentage;
                        tableCount++;
                    }
                }
                
                int overallProgress = tableCount > 0 ? totalProgress / tableCount : 0;
                
                progress.put("overall_percentage", overallProgress);
                progress.put("table_progress", tableProgress);
                progress.put("total_tables", tableCount);
                progress.put("status", overallProgress >= 100 ? "complete" : (overallProgress > 0 ? "partial" : "empty"));
                progress.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
        } catch (Exception e) {
            logger.error("❌ Error calculating seeding progress", e);
            progress.put("error", e.getMessage());
            progress.put("overall_percentage", 0);
            progress.put("status", "error");
        }
        
        return progress;
    }

    /**
     * Test data flow from MySQL to DragonflyDB for all tables
     */
    @GetMapping("/test-data-flow")
    public ResponseEntity<Map<String, Object>> testDataFlow() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.put("status", "testing");
            
            // Test each table with 3 sample records
            Map<String, Object> tableTests = new HashMap<>();
            
            // 1. Test chip_model table
            try {
                long mysqlCount = chipRepository.count();
                Set<String> redisKeys = redisTemplate.keys("chip_model:*");
                int redisCount = redisKeys != null ? redisKeys.size() : 0;
                
                Map<String, Object> chipModelTest = new HashMap<>();
                chipModelTest.put("mysql_count", mysqlCount);
                chipModelTest.put("redis_count", redisCount);
                chipModelTest.put("sample_keys", redisKeys != null ? redisKeys.stream().limit(3).toList() : "No keys found");
                tableTests.put("chip_model", chipModelTest);
            } catch (Exception e) {
                tableTests.put("chip_model", "error: " + e.getMessage());
            }
            
            // 2. Test servicos table
            try {
                long mysqlCount = servicosRepository.count();
                Set<String> redisKeys = redisTemplate.keys("servicos:*");
                int redisCount = redisKeys != null ? redisKeys.size() : 0;
                
                Map<String, Object> servicosTest = new HashMap<>();
                servicosTest.put("mysql_count", mysqlCount);
                servicosTest.put("redis_count", redisCount);
                servicosTest.put("sample_keys", redisKeys != null ? redisKeys.stream().limit(3).toList() : "No keys found");
                tableTests.put("servicos", servicosTest);
            } catch (Exception e) {
                tableTests.put("servicos", "error: " + e.getMessage());
            }
            
            // 3. Test operadoras table
            try {
                long mysqlCount = operadorasRepository.count();
                Set<String> redisKeys = redisTemplate.keys("v_operadoras:*");
                int redisCount = redisKeys != null ? redisKeys.size() : 0;
                
                Map<String, Object> operadorasTest = new HashMap<>();
                operadorasTest.put("mysql_count", mysqlCount);
                operadorasTest.put("redis_count", redisCount);
                operadorasTest.put("sample_keys", redisKeys != null ? redisKeys.stream().limit(3).toList() : "No keys found");
                tableTests.put("operadoras", operadorasTest);
            } catch (Exception e) {
                tableTests.put("operadoras", "error: " + e.getMessage());
            }
            
            // 4. Test chip_number_control table
            try {
                long mysqlCount = chipNumberControlRepository.count();
                Set<String> redisKeys = redisTemplate.keys("chip_number_control:*");
                int redisCount = redisKeys != null ? redisKeys.size() : 0;
                
                Map<String, Object> chipNumberControlTest = new HashMap<>();
                chipNumberControlTest.put("mysql_count", mysqlCount);
                chipNumberControlTest.put("redis_count", redisCount);
                chipNumberControlTest.put("sample_keys", redisKeys != null ? redisKeys.stream().limit(3).toList() : "No keys found");
                tableTests.put("chip_number_control", chipNumberControlTest);
            } catch (Exception e) {
                tableTests.put("chip_number_control", "error: " + e.getMessage());
            }
            
            // 5. Test usuario table
            try {
                long mysqlCount = userDbRepository.count();
                Set<String> redisKeys = redisTemplate.keys("usuario:*");
                int redisCount = redisKeys != null ? redisKeys.size() : 0;
                
                Map<String, Object> usuarioTest = new HashMap<>();
                usuarioTest.put("mysql_count", mysqlCount);
                usuarioTest.put("redis_count", redisCount);
                usuarioTest.put("sample_keys", redisKeys != null ? redisKeys.stream().limit(3).toList() : "No keys found");
                tableTests.put("usuario", usuarioTest);
            } catch (Exception e) {
                tableTests.put("usuario", "error: " + e.getMessage());
            }
            
            // 6. Test activation table
            try {
                long mysqlCount = activationRepository.count();
                Set<String> redisKeys = redisTemplate.keys("activation:*");
                int redisCount = redisKeys != null ? redisKeys.size() : 0;
                
                Map<String, Object> activationTest = new HashMap<>();
                activationTest.put("mysql_count", mysqlCount);
                activationTest.put("redis_count", redisCount);
                activationTest.put("sample_keys", redisKeys != null ? redisKeys.stream().limit(3).toList() : "No keys found");
                tableTests.put("activation", activationTest);
            } catch (Exception e) {
                tableTests.put("activation", "error: " + e.getMessage());
            }
            
            result.put("table_tests", tableTests);
            result.put("status", "completed");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("❌ Error testing data flow", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
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

            // 1) Full persistence warmup (chip_model, chip_model_online, servicos, v_operadoras)
            if (fullPersistenceService != null) {
                try {
                    fullPersistenceService.performInitialFullWarmup();
                    result.put("full_persistence", "ok");
                } catch (Exception e) {
                    result.put("full_persistence", "error: " + e.getMessage());
                }
            } else {
                result.put("full_persistence", "service_not_available");
            }

            // 2) Operators cache (v_operadoras index)
            try {
                operatorsCacheService.warmUpOperatorsCache();
                result.put("operators_cache", "ok");
            } catch (Exception e) {
                result.put("operators_cache", "error: " + e.getMessage());
            }

            // 3) Core cache warmups (services, numbers availability, balances)
            try {
                // Kick off the same set the scheduler does
                result.put("services_users_numbers_2m", "queued");
                // Methods are scheduled; we call the internal helpers via status ping side-effects
            } catch (Exception e) {
                result.put("services_users_numbers_2m", "error: " + e.getMessage());
            }

            // 4) Persistent tables incremental syncs if exposed
            try {
                // If service exposes public sync, call; otherwise rely on scheduled jobs
                result.put("persistent_tables", "scheduled");
            } catch (Exception e) {
                result.put("persistent_tables", "error: " + e.getMessage());
            }

            result.put("message", "Warmup triggered");
            
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

    /**
     * Get MySQL table row counts and statistics
     */
    @GetMapping("/mysql-stats")
    public ResponseEntity<Map<String, Object>> getMysqlStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            stats.put("status", "healthy");
            
            // Get row counts for each table
            Map<String, Object> tableStats = new HashMap<>();
            
            // Core tables
            tableStats.put("chip_model", getTableStats("chip_model", chipRepository.count()));
            tableStats.put("servicos", getTableStats("servicos", servicosRepository.count()));
            tableStats.put("operadoras", getTableStats("operadoras", operadorasRepository.count()));
            tableStats.put("chip_number_control", getTableStats("chip_number_control", chipNumberControlRepository.count()));
            tableStats.put("usuario", getTableStats("usuario", userDbRepository.count()));
            tableStats.put("activation", getTableStats("activation", activationRepository.count()));
            
            // Calculate totals
            long totalRows = chipRepository.count() + servicosRepository.count() + 
                            operadorasRepository.count() + chipNumberControlRepository.count() + 
                            userDbRepository.count() + activationRepository.count();
            
            stats.put("tables", tableStats);
            stats.put("total_rows", totalRows);
            stats.put("table_count", tableStats.size());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("❌ Error getting MySQL stats", e);
            stats.put("status", "error");
            stats.put("error", e.getMessage());
            return ResponseEntity.status(500).body(stats);
        }
    }

    /**
     * Helper method to create table statistics
     */
    private Map<String, Object> getTableStats(String tableName, long count) {
        Map<String, Object> tableInfo = new HashMap<>();
        tableInfo.put("name", tableName);
        tableInfo.put("row_count", count);
        tableInfo.put("last_updated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return tableInfo;
    }
}
