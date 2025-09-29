package br.com.store24h.store24h.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * RedisSetService - Atomic number reservations using Redis data structures
 *
 * Implements Phase 1 Velocity Layer with:
 * - Atomic reserve/confirm/rollback for numbers using Lua scripts
 * - High-performance data structures for number pools
 * - TTL-based reservation system preventing double assignments
 *
 * Redis Key Design:
 * - available:{operator}:{service}:{country} (Set): Pool of available numbers
 * - reserved:{operator}:{service}:{country}:{token} (Set): Reserved numbers with TTL
 * - used:{service} (Set): Numbers already activated for a service
 * - activation:{id} (Hash): Activation state for fast polling
 *
 * @author PRD Implementation - Velocity Layer
 */
@Service
public class RedisSetService {

    private static final Logger logger = LoggerFactory.getLogger(RedisSetService.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // TTL configurations
    private static final Duration RESERVE_TTL = Duration.ofMinutes(5); // 5 min reserve window
    private static final Duration ACTIVATION_TTL = Duration.ofHours(24); // 24h activation tracking

    // Lua script for atomic number reservation
    private static final String RESERVE_SCRIPT = """
        -- KEYS[1]: available pool key
        -- KEYS[2]: reserved pool key
        -- ARGV[1]: reserve TTL in seconds

        local number = redis.call('SPOP', KEYS[1])
        if number then
            redis.call('SADD', KEYS[2], number)
            redis.call('EXPIRE', KEYS[2], ARGV[1])
            return number
        else
            return nil
        end
        """;

    // Lua script for atomic reserve confirmation
    private static final String CONFIRM_SCRIPT = """
        -- KEYS[1]: reserved pool key
        -- KEYS[2]: used service key
        -- KEYS[3]: available pool key
        -- ARGV[1]: number to confirm

        local exists = redis.call('SISMEMBER', KEYS[1], ARGV[1])
        if exists == 1 then
            redis.call('SREM', KEYS[1], ARGV[1])
            redis.call('SADD', KEYS[2], ARGV[1])
            -- Remove from available pool definitively
            redis.call('SREM', KEYS[3], ARGV[1])
            return 1
        else
            return 0
        end
        """;

    // Lua script for atomic reserve rollback
    private static final String ROLLBACK_SCRIPT = """
        -- KEYS[1]: reserved pool key
        -- KEYS[2]: available pool key
        -- ARGV[1]: number to rollback

        local exists = redis.call('SISMEMBER', KEYS[1], ARGV[1])
        if exists == 1 then
            redis.call('SREM', KEYS[1], ARGV[1])
            redis.call('SADD', KEYS[2], ARGV[1])
            return 1
        else
            return 0
        end
        """;

    private final DefaultRedisScript<String> reserveScript;
    private final DefaultRedisScript<Long> confirmScript;
    private final DefaultRedisScript<Long> rollbackScript;

    public RedisSetService() {
        reserveScript = new DefaultRedisScript<>(RESERVE_SCRIPT, String.class);
        confirmScript = new DefaultRedisScript<>(CONFIRM_SCRIPT, Long.class);
        rollbackScript = new DefaultRedisScript<>(ROLLBACK_SCRIPT, Long.class);
    }

    /**
     * Atomic number reservation with TTL
     * Returns reservation token for tracking
     */
    public ReservationResult reserveNumber(String operator, String service, String country) {
        long startTime = System.nanoTime();

        try {
            String availableKey = buildAvailableKey(operator, service, country);
            String token = generateReservationToken();
            String reservedKey = buildReservedKey(operator, service, country, token);

            List<String> keys = Arrays.asList(availableKey, reservedKey);
            List<String> args = Arrays.asList(String.valueOf(RESERVE_TTL.getSeconds()));

            String reservedNumber = redisTemplate.execute(reserveScript, keys, args.toArray());

            long duration = (System.nanoTime() - startTime) / 1000000;

            if (reservedNumber != null) {
                logger.debug("✅ Number reserved: {} with token {} in {}ms",
                    reservedNumber, token.substring(0, 8) + "***", duration);

                return new ReservationResult(true, reservedNumber, token, null);
            } else {
                logger.debug("❌ No numbers available for {}:{}:{} in {}ms",
                    operator, service, country, duration);

                return new ReservationResult(false, null, null, "NO_NUMBERS");
            }

        } catch (Exception e) {
            logger.error("❌ Error reserving number for {}:{}:{}", operator, service, country, e);
            return new ReservationResult(false, null, null, "ERROR_REDIS");
        }
    }

    /**
     * Confirm number reservation (finalize assignment)
     */
    public boolean confirmReservation(String operator, String service, String country,
                                     String token, String number) {
        long startTime = System.nanoTime();

        try {
            String reservedKey = buildReservedKey(operator, service, country, token);
            String usedKey = buildUsedKey(service);
            String availableKey = buildAvailableKey(operator, service, country);

            List<String> keys = Arrays.asList(reservedKey, usedKey, availableKey);
            List<String> args = Arrays.asList(number);

            Long result = redisTemplate.execute(confirmScript, keys, args.toArray());

            long duration = (System.nanoTime() - startTime) / 1000000;

            if (result != null && result == 1) {
                logger.debug("✅ Reservation confirmed for {} with token {} in {}ms",
                    number, token.substring(0, 8) + "***", duration);
                return true;
            } else {
                logger.warn("⚠️ Failed to confirm reservation for {} with token {} in {}ms",
                    number, token.substring(0, 8) + "***", duration);
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ Error confirming reservation for {} with token {}",
                number, token.substring(0, 8) + "***", e);
            return false;
        }
    }

    /**
     * Rollback number reservation (return to available pool)
     */
    public boolean rollbackReservation(String operator, String service, String country,
                                      String token, String number) {
        long startTime = System.nanoTime();

        try {
            String reservedKey = buildReservedKey(operator, service, country, token);
            String availableKey = buildAvailableKey(operator, service, country);

            List<String> keys = Arrays.asList(reservedKey, availableKey);
            List<String> args = Arrays.asList(number);

            Long result = redisTemplate.execute(rollbackScript, keys, args.toArray());

            long duration = (System.nanoTime() - startTime) / 1000000;

            if (result != null && result == 1) {
                logger.debug("✅ Reservation rolled back for {} with token {} in {}ms",
                    number, token.substring(0, 8) + "***", duration);
                return true;
            } else {
                logger.warn("⚠️ Failed to rollback reservation for {} with token {} in {}ms",
                    number, token.substring(0, 8) + "***", duration);
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ Error rolling back reservation for {} with token {}",
                number, token.substring(0, 8) + "***", e);
            return false;
        }
    }

    /**
     * Check if number is already used for a service
     */
    public boolean isNumberUsed(String service, String number) {
        try {
            String usedKey = buildUsedKey(service);
            Boolean isMember = redisTemplate.opsForSet().isMember(usedKey, number);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            logger.error("❌ Error checking if number {} is used for service {}", number, service, e);
            return true; // Fail safe - assume used
        }
    }

    /**
     * Add activation state for fast polling
     */
    public void setActivationState(Long activationId, String number, String service, String status) {
        try {
            String activationKey = buildActivationKey(activationId);
            Map<String, String> activationData = Map.of(
                "number", number,
                "service", service,
                "status", status,
                "timestamp", String.valueOf(System.currentTimeMillis())
            );

            redisTemplate.opsForHash().putAll(activationKey, activationData);
            redisTemplate.expire(activationKey, ACTIVATION_TTL);

            logger.debug("✅ Activation state set for ID: {} Number: {} Service: {}",
                activationId, number, service);

        } catch (Exception e) {
            logger.error("❌ Error setting activation state for ID: {}", activationId, e);
        }
    }

    /**
     * Get activation state for fast polling
     */
    public Map<String, String> getActivationState(Long activationId) {
        try {
            String activationKey = buildActivationKey(activationId);
            Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(activationKey);

            if (rawMap.isEmpty()) {
                return null;
            }

            Map<String, String> result = new HashMap<>();
            rawMap.forEach((k, v) -> result.put(k.toString(), v.toString()));

            return result;

        } catch (Exception e) {
            logger.error("❌ Error getting activation state for ID: {}", activationId, e);
            return null;
        }
    }

    /**
     * Populate available number pools from database data
     */
    public void populateAvailablePool(String operator, String service, String country,
                                     Set<String> numbers) {
        try {
            String availableKey = buildAvailableKey(operator, service, country);

            if (!numbers.isEmpty()) {
                redisTemplate.opsForSet().add(availableKey, numbers.toArray(new String[0]));
                logger.debug("✅ Populated {} numbers for pool {}:{}:{}",
                    numbers.size(), operator, service, country);
            }

        } catch (Exception e) {
            logger.error("❌ Error populating available pool for {}:{}:{}",
                operator, service, country, e);
        }
    }

    /**
     * Get available count for a pool
     */
    public long getAvailableCount(String operator, String service, String country) {
        try {
            String availableKey = buildAvailableKey(operator, service, country);
            Long size = redisTemplate.opsForSet().size(availableKey);
            return size != null ? size : 0L;
        } catch (Exception e) {
            logger.error("❌ Error getting available count for {}:{}:{}",
                operator, service, country, e);
            return 0L;
        }
    }

    /**
     * Get reserved count for a pool (debugging)
     */
    public long getReservedCount(String operator, String service, String country) {
        try {
            // Count all reserved tokens for this pool
            String pattern = buildReservedKeyPattern(operator, service, country);
            Set<String> reservedKeys = redisTemplate.keys(pattern);

            long totalReserved = 0;
            if (reservedKeys != null) {
                for (String key : reservedKeys) {
                    Long size = redisTemplate.opsForSet().size(key);
                    totalReserved += (size != null ? size : 0);
                }
            }

            return totalReserved;

        } catch (Exception e) {
            logger.error("❌ Error getting reserved count for {}:{}:{}",
                operator, service, country, e);
            return 0L;
        }
    }

    /**
     * Get pool statistics for monitoring
     */
    public PoolStats getPoolStats(String operator, String service, String country) {
        return new PoolStats(
            getAvailableCount(operator, service, country),
            getReservedCount(operator, service, country),
            getUsedCount(service)
        );
    }

    /**
     * Get used count for a service
     */
    public long getUsedCount(String service) {
        try {
            String usedKey = buildUsedKey(service);
            Long size = redisTemplate.opsForSet().size(usedKey);
            return size != null ? size : 0L;
        } catch (Exception e) {
            logger.error("❌ Error getting used count for service {}", service, e);
            return 0L;
        }
    }

    // Key builders
    private String buildAvailableKey(String operator, String service, String country) {
        return String.format("available:%s:%s:%s", operator, service, country);
    }

    private String buildReservedKey(String operator, String service, String country, String token) {
        return String.format("reserved:%s:%s:%s:%s", operator, service, country, token);
    }

    private String buildReservedKeyPattern(String operator, String service, String country) {
        return String.format("reserved:%s:%s:%s:*", operator, service, country);
    }

    private String buildUsedKey(String service) {
        return String.format("used:%s", service);
    }

    private String buildActivationKey(Long activationId) {
        return String.format("activation:%d", activationId);
    }

    private String generateReservationToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    // Result classes
    public static class ReservationResult {
        private final boolean success;
        private final String number;
        private final String token;
        private final String error;

        public ReservationResult(boolean success, String number, String token, String error) {
            this.success = success;
            this.number = number;
            this.token = token;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public String getNumber() { return number; }
        public String getToken() { return token; }
        public String getError() { return error; }
    }

    public static class PoolStats {
        private final long available;
        private final long reserved;
        private final long used;

        public PoolStats(long available, long reserved, long used) {
            this.available = available;
            this.reserved = reserved;
            this.used = used;
        }

        public long getAvailable() { return available; }
        public long getReserved() { return reserved; }
        public long getUsed() { return used; }
        public long getTotal() { return available + reserved + used; }
    }
}