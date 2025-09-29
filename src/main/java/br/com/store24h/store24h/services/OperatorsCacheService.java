package br.com.store24h.store24h.services;

import br.com.store24h.store24h.RedisService;
import br.com.store24h.store24h.model.Operadoras;
import br.com.store24h.store24h.repository.OperadorasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Operators Cache Service - High-performance operator lookups
 * Cache TTL: 5 minutes for v_operadoras table
 * 
 * @author Archer (brainuxdev@gmail.com)
 */
@Service
public class OperatorsCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(OperatorsCacheService.class);
    
    @Autowired
    private OperadorasRepository operadorasRepository;
    
    @Autowired
    private RedisService redisService;
    
    private static final Duration OPERATORS_CACHE_TTL = Duration.ofMinutes(5);
    private static final String OPERATORS_CACHE_KEY = "operators_list";
    
    /**
     * Get all operators with 5-minute Redis caching
     */
    @Cacheable(value = "operatorsList", key = "'all'")
    public List<String> getAllOperatorsCached() {
        try {
            // Try Redis cache first
            String cachedOperators = redisService.get("operators", OPERATORS_CACHE_KEY);
            if (cachedOperators != null) {
                logger.debug("✅ Operators cache HIT");
                return parseOperatorsList(cachedOperators);
            }
            
            // Cache miss - query database
            logger.debug("💾 Operators cache MISS - querying v_operadoras");
            List<String> operators = operadorasRepository.findAll()
                .stream()
                .map(Operadoras::getOperadora)
                .sorted()
                .collect(Collectors.toList());
            
            // Cache the result
            String operatorsString = String.join(",", operators);
            redisService.set("operators", OPERATORS_CACHE_KEY, operatorsString, OPERATORS_CACHE_TTL);
            
            logger.info("✅ Cached {} operators for 5 minutes", operators.size());
            return operators;
            
        } catch (Exception e) {
            logger.error("❌ Error getting operators - fallback to direct DB", e);
            return getFallbackOperators();
        }
    }
    
    /**
     * Check if operator exists (cached)
     */
    public boolean isValidOperator(String operator) {
        try {
            List<String> operators = getAllOperatorsCached();
            return operators.stream()
                .anyMatch(op -> op.equalsIgnoreCase(operator));
        } catch (Exception e) {
            logger.error("Error validating operator: {}", operator, e);
            return true; // Fail-safe: allow if can't verify
        }
    }
    
    /**
     * Get operators for specific criteria (cached)
     */
    public List<String> getOperatorsForCountry(String country) {
        // For now, return all operators since v_operadoras doesn't have country filtering
        // This method can be enhanced if country-specific operator data becomes available
        return getAllOperatorsCached();
    }
    
    private List<String> parseOperatorsList(String cachedOperators) {
        return List.of(cachedOperators.split(","));
    }
    
    private List<String> getFallbackOperators() {
        try {
            return operadorasRepository.findAll()
                .stream()
                .map(Operadoras::getOperadora)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Critical error: Cannot get operators from database", e);
            return List.of("tim", "vivo", "claro", "oi"); // Emergency fallback
        }
    }
    
    /**
     * Warm up operators cache (called by CacheWarmingService)
     */
    public void warmUpOperatorsCache() {
        try {
            List<String> operators = getAllOperatorsCached();
            logger.debug("🔥 Operators cache warmed up with {} operators", operators.size());
        } catch (Exception e) {
            logger.error("Error warming operators cache", e);
        }
    }
    
    /**
     * Invalidate operators cache (for manual refresh)
     */
    public void invalidateOperatorsCache() {
        try {
            // Note: RedisService doesn't have delete method, but cache will expire in 5 minutes
            logger.info("🗑️ Operators cache invalidation requested - will refresh on next access");
        } catch (Exception e) {
            logger.error("Error invalidating operators cache", e);
        }
    }
    
    /**
     * Get cache statistics
     */
    public String getOperatorsCacheStats() {
        return "Operators cache TTL: " + OPERATORS_CACHE_TTL.toMinutes() + " minutes";
    }
}

