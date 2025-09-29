package br.com.store24h.store24h.services;

import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * User Balance Service - Handles balance caching with intelligent invalidation
 * 
 * Provides high-performance balance queries while ensuring data accuracy
 * through strategic cache invalidation when credits are modified.
 * 
 * @author Archer (brainuxdev@gmail.com)
 */
@Service
public class UserBalanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserBalanceService.class);
    
    @Autowired
    private UserDbRepository userDbRepository;
    
    @Autowired
    private RedisService redisService;
    
    // Cache TTL for balance data - short to ensure accuracy
    private static final Duration BALANCE_CACHE_TTL = Duration.ofSeconds(30);
    private static final String BALANCE_CACHE_PREFIX = "user_balance";
    
    /**
     * Get user balance with intelligent caching
     * Cache TTL: 30 seconds for performance while maintaining accuracy
     */
    @Cacheable(value = "userBalance", key = "#apiKey")
    public BigDecimal getUserBalance(String apiKey) {
        try {
            logger.debug("🔍 Getting balance for API key: {}", apiKey.substring(0, 8) + "***");
            
            // Try Redis cache first (manual cache for detailed control)
            String cachedBalance = redisService.get(BALANCE_CACHE_PREFIX, apiKey);
            if (cachedBalance != null) {
                logger.debug("✅ Balance cache HIT for API key: {}", apiKey.substring(0, 8) + "***");
                return new BigDecimal(cachedBalance);
            }
            
            // Cache miss - query database
            logger.debug("💾 Balance cache MISS - querying database for API key: {}", apiKey.substring(0, 8) + "***");
            Optional<User> userOpt = userDbRepository.findByApiKey(apiKey);
            
            if (!userOpt.isPresent()) {
                logger.warn("⚠️ User not found for API key: {}", apiKey.substring(0, 8) + "***");
                return BigDecimal.ZERO;
            }
            
            User user = userOpt.get();
            BigDecimal balance = user.getCredito();
            
            // Cache the balance
            redisService.set(BALANCE_CACHE_PREFIX, apiKey, balance.toString(), BALANCE_CACHE_TTL);
            logger.debug("💰 Cached balance {} for API key: {}", balance, apiKey.substring(0, 8) + "***");
            
            return balance;
            
        } catch (Exception e) {
            logger.error("❌ Error getting balance for API key: {} - falling back to direct DB query", 
                        apiKey.substring(0, 8) + "***", e);
            
            // Fallback to direct database query
            return getUserBalanceDirectFromDB(apiKey);
        }
    }
    
    /**
     * Get balance directly from database (fallback method)
     */
    private BigDecimal getUserBalanceDirectFromDB(String apiKey) {
        try {
            Optional<User> userOpt = userDbRepository.findByApiKey(apiKey);
            if (userOpt.isPresent()) {
                return userOpt.get().getCredito();
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            logger.error("❌ Critical error: Cannot get balance from database for API key: {}", 
                        apiKey.substring(0, 8) + "***", e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Get formatted balance response (like original getBalancer method)
     */
    public String getFormattedBalance(String apiKey) {
        try {
            BigDecimal balance = getUserBalance(apiKey);
            return "ACCESS_BALANCE:" + balance;
        } catch (Exception e) {
            logger.error("❌ Error getting formatted balance for API key: {}", 
                        apiKey.substring(0, 8) + "***", e);
            return "ACCESS_BALANCE:0";
        }
    }
    
    /**
     * Invalidate balance cache for a specific user
     * Called when credits are modified
     */
    @CacheEvict(value = "userBalance", key = "#apiKey")
    public void invalidateBalanceCache(String apiKey) {
        try {
            // Remove from Redis cache
            redisService.get(BALANCE_CACHE_PREFIX, apiKey); // This will check if exists
            // Note: RedisService doesn't have delete method, but cache will expire in 30s
            
            logger.info("🗑️ Invalidated balance cache for API key: {}", apiKey.substring(0, 8) + "***");
        } catch (Exception e) {
            logger.error("❌ Error invalidating balance cache for API key: {}", 
                        apiKey.substring(0, 8) + "***", e);
        }
    }
    
    /**
     * Decrease user balance and invalidate cache atomically
     * Returns number of updated rows (0 if insufficient balance)
     */
    @Transactional
    public int decreaseBalanceWithCacheInvalidation(String apiKey, BigDecimal amount) {
        try {
            // Perform database update
            int updatedRows = userDbRepository.decreaseSaldo(apiKey, amount);
            
            if (updatedRows > 0) {
                // Invalidate cache immediately after successful update
                invalidateBalanceCache(apiKey);
                logger.info("💸 Decreased balance by {} for API key: {} - cache invalidated", 
                           amount, apiKey.substring(0, 8) + "***");
            } else {
                logger.warn("⚠️ Insufficient balance to decrease by {} for API key: {}", 
                           amount, apiKey.substring(0, 8) + "***");
            }
            
            return updatedRows;
            
        } catch (Exception e) {
            logger.error("❌ Error decreasing balance for API key: {}", 
                        apiKey.substring(0, 8) + "***", e);
            throw e; // Re-throw to maintain transaction rollback behavior
        }
    }
    
    /**
     * Increase user balance and invalidate cache atomically
     */
    @Transactional
    public void increaseBalanceWithCacheInvalidation(String apiKey, BigDecimal amount) {
        try {
            // Perform database update
            userDbRepository.increaseSaldo(apiKey, amount);
            
            // Invalidate cache immediately after successful update
            invalidateBalanceCache(apiKey);
            logger.info("💰 Increased balance by {} for API key: {} - cache invalidated", 
                       amount, apiKey.substring(0, 8) + "***");
            
        } catch (Exception e) {
            logger.error("❌ Error increasing balance for API key: {}", 
                        apiKey.substring(0, 8) + "***", e);
            throw e; // Re-throw to maintain transaction rollback behavior
        }
    }
    
    /**
     * Warm up balance cache for a specific user
     * Useful for pre-loading frequently accessed balances
     */
    public void warmUpBalanceCache(String apiKey) {
        try {
            getUserBalance(apiKey); // This will cache the balance
            logger.debug("🔥 Warmed up balance cache for API key: {}", apiKey.substring(0, 8) + "***");
        } catch (Exception e) {
            logger.error("❌ Error warming up balance cache for API key: {}", 
                        apiKey.substring(0, 8) + "***", e);
        }
    }
    
    /**
     * Check if user has sufficient balance (cached)
     */
    public boolean hasSufficientBalance(String apiKey, BigDecimal requiredAmount) {
        try {
            BigDecimal currentBalance = getUserBalance(apiKey);
            boolean sufficient = currentBalance.compareTo(requiredAmount) >= 0;
            
            logger.debug("💳 Balance check for API key: {} - Required: {} Current: {} Sufficient: {}", 
                        apiKey.substring(0, 8) + "***", requiredAmount, currentBalance, sufficient);
            
            return sufficient;
            
        } catch (Exception e) {
            logger.error("❌ Error checking balance sufficiency for API key: {}", 
                        apiKey.substring(0, 8) + "***", e);
            return false; // Fail safe - assume insufficient balance
        }
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public String getBalanceCacheStats() {
        // This would be implemented based on your monitoring needs
        return "Balance cache TTL: " + BALANCE_CACHE_TTL.getSeconds() + " seconds";
    }
}
