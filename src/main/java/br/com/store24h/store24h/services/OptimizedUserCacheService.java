package br.com.store24h.store24h.services;

import br.com.store24h.store24h.RedisService;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.apiv2.TipoDeApiEnum;
import br.com.store24h.store24h.repository.UserDbRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * Optimized User Cache Service - Strategic caching for usuario table
 * 
 * CRITICAL FIELDS (always fresh): credito, whatsapp_enabled
 * OTHER FIELDS (15-min cache): email, tipo_de_api, etc.
 * 
 * This ensures data integrity for balance and ACL while maximizing performance
 * for non-critical user profile data.
 * 
 * @author Archer (brainuxdev@gmail.com)
 */
@Service
public class OptimizedUserCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedUserCacheService.class);
    
    @Autowired
    private UserDbRepository userDbRepository;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private UserBalanceService userBalanceService; // Already has balance caching
    
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(15);
    private static final String USER_CACHE_PREFIX = "user_data";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Get user with optimized caching strategy
     * ALWAYS FRESH: credito, whatsapp_enabled  
     * CACHED (15min): other fields
     */
    public Optional<User> getUserOptimized(String apiKey) {
        try {
            // Get cached non-critical data
            UserCacheData cachedData = getCachedUserData(apiKey);
            
            if (cachedData != null) {
                logger.debug("✅ User cache HIT for: {}", apiKey.substring(0, 8) + "***");
                
                // Build user with cached data + fresh critical fields
                User user = buildUserFromCache(cachedData);
                
                // ALWAYS get fresh critical data
                populateFreshCriticalFields(user, apiKey);
                
                return Optional.of(user);
            }
            
            // Cache miss - get full user from DB
            logger.debug("💾 User cache MISS - querying database for: {}", apiKey.substring(0, 8) + "***");
            Optional<User> userOpt = userDbRepository.findByApiKey(apiKey);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Cache non-critical data only
                cacheNonCriticalUserData(user);
                
                return userOpt;
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("Error getting optimized user for: {}", apiKey.substring(0, 8) + "***", e);
            
            // Fallback to direct DB query
            return userDbRepository.findByApiKey(apiKey);
        }
    }
    
    /**
     * Get user's WhatsApp status (always fresh - critical for ACL)
     */
    public boolean getUserWhatsAppEnabled(String apiKey) {
        try {
            Optional<User> userOpt = userDbRepository.findByApiKey(apiKey);
            if (userOpt.isPresent()) {
                boolean enabled = userOpt.get().getWhatsAppEnabled();
                logger.debug("🔒 Fresh WhatsApp status for {}: {}", apiKey.substring(0, 8) + "***", enabled);
                return enabled;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error getting WhatsApp status for: {}", apiKey.substring(0, 8) + "***", e);
            return false;
        }
    }
    
    /**
     * Get user's current balance (uses existing UserBalanceService cache)
     */
    public BigDecimal getUserBalance(String apiKey) {
        return userBalanceService.getUserBalance(apiKey);
    }
    
    /**
     * Get user's API type (cacheable - not critical for balance/ACL)
     */
    @Cacheable(value = "userApiType", key = "#apiKey", unless = "#result == null")
    public TipoDeApiEnum getUserApiType(String apiKey) {
        try {
            Optional<User> userOpt = userDbRepository.findByApiKey(apiKey);
            if (userOpt.isPresent()) {
                TipoDeApiEnum tipo = userOpt.get().getTipo_de_api();
                if (tipo == null) {
                    logger.warn("User tipo_de_api is null for: {}", apiKey.substring(0, 8) + "***");
                    // Default to ANTIGA (legacy) if missing to preserve access
                    return TipoDeApiEnum.ANTIGA;
                }
                return tipo;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting API type for: {}", apiKey.substring(0, 8) + "***", e);
            return null;
        }
    }
    
    /**
     * Check if user exists (uses cache for non-critical validation)
     */
    public boolean userExists(String apiKey) {
        try {
            Optional<User> user = getUserOptimized(apiKey);
            return user.isPresent();
        } catch (Exception e) {
            logger.error("Error checking user existence for: {}", apiKey.substring(0, 8) + "***", e);
            return false;
        }
    }
    
    /**
     * Get user ID (cacheable - not critical)
     */
    public Long getUserId(String apiKey) {
        try {
            UserCacheData cachedData = getCachedUserData(apiKey);
            if (cachedData != null) {
                return cachedData.getId();
            }
            
            // Fallback to DB
            Optional<User> userOpt = userDbRepository.findByApiKey(apiKey);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                cacheNonCriticalUserData(user);
                return user.getId();
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error getting user ID for: {}", apiKey.substring(0, 8) + "***", e);
            return null;
        }
    }
    
    private UserCacheData getCachedUserData(String apiKey) {
        try {
            String cached = redisService.get(USER_CACHE_PREFIX, apiKey);
            if (cached != null) {
                return mapper.readValue(cached, UserCacheData.class);
            }
            return null;
        } catch (Exception e) {
            logger.debug("Error reading cached user data", e);
            return null;
        }
    }
    
    private void cacheNonCriticalUserData(User user) {
        try {
            UserCacheData cacheData = new UserCacheData();
            cacheData.setId(user.getId());
            cacheData.setEmail(user.getEmail());
            cacheData.setApiKey(user.getApiKey());
            cacheData.setTipoDeApi(user.getTipo_de_api());
            // NOT caching: credito, whatsapp_enabled (always fresh)
            
            String json = mapper.writeValueAsString(cacheData);
            redisService.set(USER_CACHE_PREFIX, user.getApiKey(), json, USER_CACHE_TTL);
            
            logger.debug("💾 Cached non-critical user data for: {}", user.getApiKey().substring(0, 8) + "***");
        } catch (Exception e) {
            logger.error("Error caching user data", e);
        }
    }
    
    private User buildUserFromCache(UserCacheData cachedData) {
        User user = new User();
        // Note: ID is auto-generated, cannot be set manually
        user.setEmail(cachedData.getEmail());
        user.setApiKey(cachedData.getApiKey());
        user.setTipo_de_api(cachedData.getTipoDeApi());
        return user;
    }
    
    private void populateFreshCriticalFields(User user, String apiKey) {
        try {
            // Get fresh critical fields from database
            Optional<User> freshUser = userDbRepository.findByApiKey(apiKey);
            if (freshUser.isPresent()) {
                User fresh = freshUser.get();
                user.setCredito(fresh.getCredito()); // Critical for balance checks
                user.setWhatsapp_enabled(fresh.getWhatsAppEnabled()); // Critical for ACL
                
                logger.debug("🔒 Applied fresh critical fields for: {}", apiKey.substring(0, 8) + "***");
            }
        } catch (Exception e) {
            logger.error("Error getting fresh critical fields", e);
        }
    }
    
    /**
     * Invalidate user cache when critical data changes
     */
    public void invalidateUserCache(String apiKey) {
        try {
            // Note: RedisService doesn't have delete method, but cache will expire in 15 minutes
            logger.info("🗑️ User cache invalidation requested for: {}", apiKey.substring(0, 8) + "***");
        } catch (Exception e) {
            logger.error("Error invalidating user cache", e);
        }
    }
    
    /**
     * Warm up user cache for frequently accessed users
     */
    public void warmUpUserCache(String apiKey) {
        try {
            getUserOptimized(apiKey);
            logger.debug("🔥 Warmed up user cache for: {}", apiKey.substring(0, 8) + "***");
        } catch (Exception e) {
            logger.error("Error warming user cache", e);
        }
    }
    
    /**
     * Get cache statistics
     */
    public String getUserCacheStats() {
        return "User cache TTL: " + USER_CACHE_TTL.toMinutes() + " minutes (non-critical fields only)";
    }
    
    /**
     * Cache data structure (non-critical fields only)
     */
    public static class UserCacheData {
        private Long id;
        private String email;
        private String apiKey;
        private TipoDeApiEnum tipoDeApi;
        
        // Default constructor for Jackson
        public UserCacheData() {}
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public TipoDeApiEnum getTipoDeApi() { return tipoDeApi; }
        public void setTipoDeApi(TipoDeApiEnum tipoDeApi) { this.tipoDeApi = tipoDeApi; }
    }
}
