package br.com.store24h.store24h.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisSetService {
    private String normalizeOperator(String operator) {
        return operator == null ? null : operator.toLowerCase();
    }
    
    // Inner classes for return types
    public static class ReservationResult {
        private final boolean success;
        private final String number;
        private final String error;
        private final String token;
        
        public ReservationResult(boolean success, String number, String error) {
            this.success = success;
            this.number = number;
            this.error = error;
            this.token = number; // Use number as token for compatibility
        }
        
        public ReservationResult(boolean success, String number, String error, String token) {
            this.success = success;
            this.number = number;
            this.error = error;
            this.token = token;
        }
        
        public boolean isSuccess() { return success; }
        public String getNumber() { return number; }
        public String getError() { return error; }
        public String getToken() { return token; }
    }
    
    public static class PoolStats {
        private final long available;
        private final long used;
        private final long total;
        private final long reserved;
        
        public PoolStats(long available, long used, long total) {
            this.available = available;
            this.used = used;
            this.total = total;
            this.reserved = used; // For compatibility
        }
        
        public PoolStats(long available, long used, long total, long reserved) {
            this.available = available;
            this.used = used;
            this.total = total;
            this.reserved = reserved;
        }
        
        public long getAvailable() { return available; }
        public long getUsed() { return used; }
        public long getTotal() { return total; }
        public long getReserved() { return reserved; }
    }
    
    private static final Logger logger = LoggerFactory.getLogger(RedisSetService.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private SetOperations<String, Object> setOps;
    private ValueOperations<String, Object> valueOps;
    
    public RedisSetService() {
        // Initialize operations in post-construct
    }
    
    @PostConstruct
    public void init() {
        this.setOps = redisTemplate.opsForSet();
        this.valueOps = redisTemplate.opsForValue();
    }
    
    /**
     * Add a number to the used numbers set for a specific service
     */
    public void addUsedNumber(String serviceId, String number) {
        try {
            String key = "used_numbers:{" + serviceId + "}";
            setOps.add(key, number);
            logger.debug("Added number {} to used set for service {}", number, serviceId);
        } catch (Exception e) {
            logger.error("Error adding used number {} for service {}: {}", number, serviceId, e.getMessage());
        }
    }
    
    /**
     * Check if a number is already used for a specific service
     */
    public boolean isNumberUsed(String serviceId, String number) {
        try {
            String key = "used_numbers:{" + serviceId + "}";
            Boolean isMember = setOps.isMember(key, number);
            return isMember != null && isMember;
        } catch (Exception e) {
            logger.error("Error checking if number {} is used for service {}: {}", number, serviceId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get available count for a service pool (using precomputed counter)
     */
    public long getAvailableCount(String serviceId, String country, String operator) {
        try {
            String key = "pool_count:{" + serviceId + "}:" + country + ":" + operator;
            Object count = valueOps.get(key);
            if (count != null) {
                return Long.parseLong(count.toString());
            }
            
            // Fallback: calculate from set difference
            String availableKey = "available_numbers:{" + serviceId + "}:" + country + ":" + operator;
            String usedKey = "used_numbers:{" + serviceId + "}";
            
            Long availableSize = setOps.size(availableKey);
            Long usedSize = setOps.size(usedKey);
            
            if (availableSize != null && usedSize != null) {
                return Math.max(0, availableSize - usedSize);
            }
            
            return 0;
        } catch (Exception e) {
            logger.error("Error getting available count for service {}: {}", serviceId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Reserve a number atomically using Lua script.
     * Parameters order is (operator, service, country) to match call sites.
     */
    public ReservationResult reserveNumber(String operator, String service, String country) {
        try {
            operator = normalizeOperator(operator);
            String availableKey = "available_numbers:{" + service + "}:" + country + ":" + operator;
            String usedKey = "used_numbers:{" + service + "}";

            String luaScript =
                "local available = redis.call('SPOP', KEYS[1]) " +
                "if available then " +
                "  redis.call('SADD', KEYS[2], available) " +
                "  redis.call('DECR', KEYS[3]) " +
                "  return available " +
                "else " +
                "  return nil " +
                "end";

            String countKey = "pool_count:{" + service + "}:" + country + ":" + operator;

            Object result = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Object>) connection ->
                    connection.eval(
                        luaScript.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.VALUE,
                        3,
                        availableKey.getBytes(),
                        usedKey.getBytes(),
                        countKey.getBytes()
                    )
            );

            if (result != null) {
                String reservedNumber = new String((byte[]) result);
                logger.info("Reserved number {} for {}:{}:{}", reservedNumber, service, country, operator);
                return new ReservationResult(true, reservedNumber, null, reservedNumber);
            }

            return new ReservationResult(false, null, "NO_NUMBERS");
        } catch (Exception e) {
            logger.error("Error reserving number for {}:{}:{}: {}", service, country, operator, e.getMessage());
            return new ReservationResult(false, null, e.getMessage());
        }
    }
    
    /**
     * Backward-compatible helper: delegates to new signature (operator, service, country)
     */
    public ReservationResult reserveNumberWithResult(String serviceId, String country, String operator) {
        return reserveNumber(operator, serviceId, country);
    }
    
    /**
     * Populate available pool (alias for addToPool)
     */
    public void populateAvailablePool(String serviceId, String country, String operator, Set<String> numbers) {
        addToPool(serviceId, country, normalizeOperator(operator), numbers);
    }
    
    /**
     * Populate available pool with swap (atomic operation)
     */
    public void populateAvailablePoolSwap(String serviceId, String country, String operator, Set<String> numbers) {
        try {
            operator = normalizeOperator(operator);
            String oldKey = "available_numbers:{" + serviceId + "}:" + country + ":" + operator;
            String newKey = "available_numbers_new:{" + serviceId + "}:" + country + ":" + operator;
            String countKey = "pool_count:{" + serviceId + "}:" + country + ":" + operator;
            
            // Add numbers to new key
            if (!numbers.isEmpty()) {
                setOps.add(newKey, numbers.toArray());
            }
            
            // Atomic swap
            String luaScript = 
                "redis.call('RENAME', KEYS[2], KEYS[1]) " +
                "redis.call('SET', KEYS[3], ARGV[1]) " +
                "return 'OK'";
            
            redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                    return connection.eval(
                        luaScript.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.STATUS,
                        3,
                        oldKey.getBytes(),
                        newKey.getBytes(),
                        countKey.getBytes(),
                        String.valueOf(numbers.size()).getBytes()
                    );
                }
            );
            
            String logService = serviceId != null ? serviceId.toLowerCase() : null;
            String logCountry = country != null ? country.toLowerCase() : null;
            String logOperator = operator != null ? operator.toLowerCase() : null;
            logger.info("🔁 s.RedisSetService : Swapped pool for service {}:{}:{} with {} numbers", 
                logService, logCountry, logOperator, numbers.size());
                
        } catch (Exception e) {
            logger.error("Error swapping pool for service {}:{}:{}: {}", 
                serviceId, country, operator, e.getMessage());
        }
    }
    
    /**
     * Get pool statistics
     */
    public PoolStats getPoolStats(String serviceId, String country, String operator) {
        try {
            operator = normalizeOperator(operator);
            String availableKey = "available_numbers:{" + serviceId + "}:" + country + ":" + operator;
            String usedKey = "used_numbers:{" + serviceId + "}";
            
            Long available = setOps.size(availableKey);
            Long used = setOps.size(usedKey);
            
            long availableCount = available != null ? available : 0L;
            long usedCount = used != null ? used : 0L;
            long totalCount = availableCount + usedCount;
            
            return new PoolStats(availableCount, usedCount, totalCount);
        } catch (Exception e) {
            logger.error("Error getting pool stats for service {}:{}:{}: {}", 
                serviceId, country, operator, e.getMessage());
            return new PoolStats(0, 0, 0);
        }
    }
    
    /**
     * Confirm reservation (mark as permanently assigned) - returns boolean
     */
    public boolean confirmReservation(String serviceId, String number, String country, String operator, String userId) {
        try {
            operator = normalizeOperator(operator);
            // Mark as confirmed in Redis
            String confirmedKey = "confirmed:{" + serviceId + "}:" + number + ":" + country + ":" + operator;
            valueOps.set(confirmedKey, userId, 86400, TimeUnit.SECONDS); // 24 hours TTL
            
            logger.debug("Confirmed reservation for {}:{}:{}:{}", serviceId, number, country, operator);
            return true;
        } catch (Exception e) {
            logger.error("Error confirming reservation for {}:{}:{}:{}: {}", 
                serviceId, number, country, operator, e.getMessage());
            return false;
        }
    }
    
    /**
     * Rollback reservation (return number to available pool) - returns boolean
     */
    public boolean rollbackReservation(String serviceId, String number, String country, String operator, String userId) {
        try {
            operator = normalizeOperator(operator);
            String availableKey = "available_numbers:{" + serviceId + "}:" + country + ":" + operator;
            String usedKey = "used_numbers:{" + serviceId + "}";
            String countKey = "pool_count:{" + serviceId + "}:" + country + ":" + operator;
            
            // Remove from used and add back to available
            setOps.remove(usedKey, number);
            setOps.add(availableKey, number);
            
            // Update counter
            Long currentCount = valueOps.get(countKey) != null ? 
                Long.parseLong(valueOps.get(countKey).toString()) : 0L;
            valueOps.set(countKey, currentCount + 1);
            
            logger.debug("Rolled back reservation for {}:{}:{}:{}", serviceId, number, country, operator);
            return true;
        } catch (Exception e) {
            logger.error("Error rolling back reservation for {}:{}:{}:{}: {}", 
                serviceId, number, country, operator, e.getMessage());
            return false;
        }
    }
    
    /**
     * Set activation state
     */
    public void setActivationState(long activationId, String serviceId, String number, String status) {
        try {
            String activationKey = "activation:{" + serviceId + "}:" + activationId;
            Map<String, Object> activationData = new HashMap<>();
            activationData.put("service_id", serviceId);
            activationData.put("number", number);
            activationData.put("status", status);
            activationData.put("timestamp", System.currentTimeMillis());
            
            redisTemplate.opsForHash().putAll(activationKey, activationData);
            redisTemplate.expire(activationKey, 86400, TimeUnit.SECONDS); // 24 hours TTL
            
            logger.debug("Set activation state for {}:{}:{}", activationId, serviceId, number);
        } catch (Exception e) {
            logger.error("Error setting activation state for {}:{}:{}: {}", 
                activationId, serviceId, number, e.getMessage());
        }
    }
    
    /**
     * Add numbers to available pool
     */
    public void addToPool(String serviceId, String country, String operator, Set<String> numbers) {
        try {
            operator = normalizeOperator(operator);
            String key = "available_numbers:{" + serviceId + "}:" + country + ":" + operator;
            String countKey = "pool_count:{" + serviceId + "}:" + country + ":" + operator;
            
            if (!numbers.isEmpty()) {
                setOps.add(key, numbers.toArray());
                
                // Update counter
                Long currentCount = valueOps.get(countKey) != null ? 
                    Long.parseLong(valueOps.get(countKey).toString()) : 0L;
                valueOps.set(countKey, currentCount + numbers.size());
                
                logger.info("Added {} numbers to pool for service {}:{}:{}", 
                    numbers.size(), serviceId, country, operator);
            }
        } catch (Exception e) {
            logger.error("Error adding numbers to pool for service {}:{}:{}: {}", 
                serviceId, country, operator, e.getMessage());
        }
    }
    
    /**
     * Remove numbers from available pool
     */
    public void removeFromPool(String serviceId, String country, String operator, Set<String> numbers) {
        try {
            operator = normalizeOperator(operator);
            String key = "available_numbers:{" + serviceId + "}:" + country + ":" + operator;
            String countKey = "pool_count:{" + serviceId + "}:" + country + ":" + operator;
            
            if (!numbers.isEmpty()) {
                Long removed = setOps.remove(key, numbers.toArray());
                
                if (removed != null && removed > 0) {
                    // Update counter
                    Long currentCount = valueOps.get(countKey) != null ? 
                        Long.parseLong(valueOps.get(countKey).toString()) : 0L;
                    valueOps.set(countKey, Math.max(0, currentCount - removed));
                    
                    logger.info("Removed {} numbers from pool for service {}:{}:{}", 
                        removed, serviceId, country, operator);
                }
            }
        } catch (Exception e) {
            logger.error("Error removing numbers from pool for service {}:{}:{}: {}", 
                serviceId, country, operator, e.getMessage());
        }
    }
    
    /**
     * Check if a combination is activated
     */
    public boolean isActivated(String serviceId, String number, String country, String operator) {
        try {
            operator = normalizeOperator(operator);
            String key = "activated:{" + serviceId + "}:" + number + ":" + country + ":" + operator;
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            logger.error("Error checking activation for {}:{}:{}:{}: {}", 
                serviceId, number, country, operator, e.getMessage());
            return false;
        }
    }
    
    /**
     * Mark a combination as activated
     */
    public void markActivated(String serviceId, String number, String country, String operator, long ttlSeconds) {
        try {
            operator = normalizeOperator(operator);
            String key = "activated:{" + serviceId + "}:" + number + ":" + country + ":" + operator;
            valueOps.set(key, "1", ttlSeconds, TimeUnit.SECONDS);
            logger.debug("Marked {}:{}:{}:{} as activated", serviceId, number, country, operator);
        } catch (Exception e) {
            logger.error("Error marking activation for {}:{}:{}:{}: {}", 
                serviceId, number, country, operator, e.getMessage());
        }
    }
    
    /**
     * Get pool size for a service
     */
    public long getPoolSize(String serviceId, String country, String operator) {
        try {
            operator = normalizeOperator(operator);
            String key = "available_numbers:{" + serviceId + "}:" + country + ":" + operator;
            Long size = setOps.size(key);
            return size != null ? size : 0L;
        } catch (Exception e) {
            logger.error("Error getting pool size for service {}:{}:{}: {}", 
                serviceId, country, operator, e.getMessage());
            return 0L;
        }
    }
    
    /**
     * Clear all data for a service (for testing/cleanup)
     */
    public void clearServiceData(String serviceId) {
        try {
            Set<String> keys = redisTemplate.keys("used_numbers:{" + serviceId + "}*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            keys = redisTemplate.keys("available_numbers:{" + serviceId + "}*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            keys = redisTemplate.keys("pool_count:{" + serviceId + "}*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            keys = redisTemplate.keys("activated:{" + serviceId + "}*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            logger.info("Cleared all Redis data for service {}", serviceId);
        } catch (Exception e) {
            logger.error("Error clearing service data for {}: {}", serviceId, e.getMessage());
        }
    }
}