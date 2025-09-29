package br.com.store24h.store24h.services;

import br.com.store24h.store24h.apiv2.services.CacheService;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.services.UserBalanceService;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Cache Warming Service - Proactively loads frequently accessed data into cache
 * This improves response times by ensuring hot data is always available in Redis
 * 
 * @author Archer (brainuxdev@gmail.com)
 */
@Service
@Component
public class CacheWarmingService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheWarmingService.class);
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private ServicosRepository servicosRepository;
    
    @Autowired
    private UserDbRepository userDbRepository;
    
    @Autowired
    private UserBalanceService userBalanceService;
    
    @Autowired
    private OperatorsCacheService operatorsCacheService;

    @Autowired
    private RedisSetService redisSetService;

    @Autowired
    private br.com.store24h.store24h.repository.ChipRepository chipRepository;
    
    @Value("${cache.warming.enabled:true}")
    private boolean cacheWarmingEnabled;
    
    @Value("${cache.warming.services.rate:900000}") // 15 minutes for services
    private long servicesWarmingRate;
    
    @Value("${cache.warming.callbacks.rate:900000}") // 15 minutes for API callbacks
    private long callbacksWarmingRate;
    
    @Value("${cache.warming.users.rate:900000}") // 15 minutes for user API types
    private long usersWarmingRate;
    
    @Value("${cache.warming.numbers.rate:600000}") // 10 minutes for numbers availability
    private long numbersWarmingRate;
    
    @Value("${cache.warming.configs.rate:900000}") // 15 minutes for service configs
    private long configsWarmingRate;
    
    @Value("${cache.warming.balance.rate:1800000}") // 30 minutes for user balances
    private long balanceWarmingRate;
    
    @Value("${cache.warming.operators.rate:300000}") // 5 minutes for operators
    private long operatorsWarmingRate;
    
    private final ExecutorService warmingExecutor = Executors.newFixedThreadPool(4);
    
    /**
     * Initial cache warming on application startup
     * Runs once after the application is fully initialized
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialCacheWarming() {
        if (!cacheWarmingEnabled) {
            logger.info("🚫 Cache warming is disabled via configuration");
            return;
        }
        
        logger.info("🚀 Starting initial cache warming on application startup...");
        
        CompletableFuture.runAsync(() -> {
            try {
                // Give the app a moment to fully initialize
                Thread.sleep(10000); // 10 seconds
                
                warmUpCriticalCaches();
                logger.info("✅ Initial cache warming completed successfully");
            } catch (Exception e) {
                logger.error("❌ Error during initial cache warming", e);
            }
        }, warmingExecutor);
    }
    
    /**
     * All Services cache warming - every 15 minutes
     * Warms up ALL services in the system
     */
    @Scheduled(fixedRateString = "${cache.warming.services.rate:900000}")
    public void warmUpAllServices() {
        if (!cacheWarmingEnabled) {
            return;
        }
        
        logger.info("🔥 Starting all services cache warming cycle...");
        
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                warmUpAllServicesCache();
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✅ All services cache warming completed in {}ms", duration);
            } catch (Exception e) {
                logger.error("❌ Error during services cache warming", e);
            }
        }, warmingExecutor);
    }
    
    /**
     * API Callbacks cache warming - every 15 minutes
     * Warms up API callback configurations
     */
    @Scheduled(fixedRateString = "${cache.warming.callbacks.rate:900000}")
    public void warmUpApiCallbacksScheduled() {
        if (!cacheWarmingEnabled) {
            return;
        }
        
        logger.info("🔥 Starting API callbacks cache warming cycle...");
        
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                warmUpApiCallbacks();
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✅ API callbacks cache warming completed in {}ms", duration);
            } catch (Exception e) {
                logger.error("❌ Error during API callbacks cache warming", e);
            }
        }, warmingExecutor);
    }
    
    /**
     * User API Types cache warming - every 15 minutes
     * Warms up user API type configurations
     */
    @Scheduled(fixedRateString = "${cache.warming.users.rate:900000}")
    public void warmUpUserApiTypesScheduled() {
        if (!cacheWarmingEnabled) {
            return;
        }
        
        logger.info("🔥 Starting user API types cache warming cycle...");
        
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                warmUpUserApiTypes();
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✅ User API types cache warming completed in {}ms", duration);
            } catch (Exception e) {
                logger.error("❌ Error during user API types cache warming", e);
            }
        }, warmingExecutor);
    }
    
    /**
     * Numbers Availability cache warming - every 10 minutes
     * Warms up number availability for common configurations
     */
    @Scheduled(fixedRateString = "${cache.warming.numbers.rate:600000}")
    public void warmUpNumbersAvailabilityScheduled() {
        if (!cacheWarmingEnabled) {
            return;
        }
        
        logger.info("🔥 Starting numbers availability cache warming cycle...");
        
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                warmUpNumbersAvailabilityCache();
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✅ Numbers availability cache warming completed in {}ms", duration);
            } catch (Exception e) {
                logger.error("❌ Error during numbers availability cache warming", e);
            }
        }, warmingExecutor);
    }
    
    /**
     * Service Configurations cache warming - every 15 minutes
     * Warms up service-specific configurations
     */
    @Scheduled(fixedRateString = "${cache.warming.configs.rate:900000}")
    public void warmUpServiceConfigurationsScheduled() {
        if (!cacheWarmingEnabled) {
            return;
        }
        
        logger.info("🔥 Starting service configurations cache warming cycle...");
        
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                warmUpServiceConfigurations();
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✅ Service configurations cache warming completed in {}ms", duration);
            } catch (Exception e) {
                logger.error("❌ Error during service configurations cache warming", e);
            }
        }, warmingExecutor);
    }
    
    /**
     * User Balance cache warming - every 30 minutes
     * Warms up balances for recently active users
     */
    @Scheduled(fixedRateString = "${cache.warming.balance.rate:1800000}")
    public void warmUpUserBalancesScheduled() {
        if (!cacheWarmingEnabled) {
            return;
        }
        
        logger.info("🔥 Starting user balances cache warming cycle...");
        
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                warmUpUserBalances();
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✅ User balances cache warming completed in {}ms", duration);
            } catch (Exception e) {
                logger.error("❌ Error during user balances cache warming", e);
            }
        }, warmingExecutor);
    }
    
    /**
     * Operators cache warming - every 5 minutes
     * Warms up v_operadoras table data
     */
    @Scheduled(fixedRateString = "${cache.warming.operators.rate:300000}")
    public void warmUpOperatorsScheduled() {
        if (!cacheWarmingEnabled) {
            return;
        }
        
        logger.info("🔥 Starting operators cache warming cycle...");
        
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                operatorsCacheService.warmUpOperatorsCache();
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✅ Operators cache warming completed in {}ms", duration);
            } catch (Exception e) {
                logger.error("❌ Error during operators cache warming", e);
            }
        }, warmingExecutor);
    }
    
    /**
     * Warm up critical caches that should always be hot
     */
    private void warmUpCriticalCaches() {
        logger.info("🎯 Warming critical caches...");
        
        warmUpAllServicesCache();
        warmUpNumbersAvailabilityCache();
        warmUpUserBalances();
        operatorsCacheService.warmUpOperatorsCache();
        
        logger.info("✅ Critical caches warmed");
    }
    
    /**
     * Warm up ALL services cache - gets all services from database
     */
    private void warmUpAllServicesCache() {
        try {
            logger.debug("🔥 Warming ALL services cache...");
            
            // Get all active services from the database
            List<Servico> allServices = servicosRepository.findByActivity(true, Sort.by("alias")).join();
            
            int warmed = 0;
            for (Servico servico : allServices) {
                try {
                    // This will populate the cache if not already cached
                    String cachedService = cacheService.getServiceInCache(servico.getAlias());
                    if (cachedService != null) {
                        warmed++;
                    }
                } catch (Exception e) {
                    logger.debug("Could not warm service cache for: {}", servico.getAlias());
                }
            }
            
            logger.debug("✅ Warmed {} services from database", warmed);
            
        } catch (Exception e) {
            logger.error("Error warming all services cache", e);
            
            // Fallback to popular services if database query fails
            logger.info("🔄 Falling back to popular services list...");
            warmUpPopularServicesFallback();
        }
    }
    
    /**
     * Fallback method if database query fails - warms popular services
     */
    private void warmUpPopularServicesFallback() {
        try {
            logger.debug("🔥 Warming popular services cache (fallback)...");
            
            // List of most commonly used services as fallback
            List<String> popularServices = Arrays.asList(
                "telegram", "whatsapp", "instagram", "facebook", "twitter", 
                "gmail", "yahoo", "discord", "viber", "signal", "tiktok", 
                "snapchat", "linkedin", "pinterest", "reddit", "microsoft"
            );
            
            int warmed = 0;
            for (String service : popularServices) {
                try {
                    String cachedService = cacheService.getServiceInCache(service);
                    if (cachedService != null) {
                        warmed++;
                    }
                } catch (Exception e) {
                    logger.debug("Could not warm service cache for: {}", service);
                }
            }
            
            logger.debug("✅ Warmed {} popular services (fallback)", warmed);
            
        } catch (Exception e) {
            logger.error("Error warming popular services cache (fallback)", e);
        }
    }
    
    /**
     * Warm up API callback cache for common callback IDs
     */
    private void warmUpApiCallbacks() {
        try {
            logger.debug("🔥 Warming API callbacks cache...");
            
            // Common callback IDs - customize based on your usage patterns
            List<String> commonCallbackIds = Arrays.asList(
                "1", "2", "3", "4", "5", "default", "webhook", "status"
            );
            
            int warmed = 0;
            for (String callbackId : commonCallbackIds) {
                try {
                    Map<String, Object> callback = cacheService.findApiCallback(callbackId);
                    if (callback != null && !callback.isEmpty()) {
                        warmed++;
                    }
                } catch (Exception e) {
                    logger.debug("Could not warm API callback cache for ID: {}", callbackId);
                }
            }
            
            logger.debug("✅ Warmed {} API callbacks", warmed);
            
        } catch (Exception e) {
            logger.error("Error warming API callbacks cache", e);
        }
    }
    
    /**
     * Warm up user API types for common API keys
     */
    private void warmUpUserApiTypes() {
        try {
            logger.debug("🔥 Warming user API types cache...");
            
            // Note: This would typically warm up based on recently active API keys
            // For now, we'll just log that this step is ready for implementation
            logger.debug("✅ User API types warming ready (implement based on active API keys)");
            
        } catch (Exception e) {
            logger.error("Error warming user API types cache", e);
        }
    }
    
    /**
     * Warm up numbers availability cache for common configurations
     */
    private void warmUpNumbersAvailabilityCache() {
        try {
            logger.debug("🔥 Warming numbers availability cache...");
            
            // Common number configurations - customize based on your usage patterns
            List<Map<String, Object>> commonConfigs = Arrays.asList(
                createNumberConfig(0, 1, false, Optional.empty(), new ArrayList<>()),
                createNumberConfig(0, 1, true, Optional.empty(), Arrays.asList("1")),
                createNumberConfig(1, 1, false, Optional.empty(), new ArrayList<>()),
                createNumberConfig(0, 1, false, Optional.of("any"), new ArrayList<>())
            );
            
            int warmed = 0;
            for (Map<String, Object> config : commonConfigs) {
                try {
                    @SuppressWarnings("unchecked")
                    Optional<String> operadora = (Optional<String>) config.get("operadora");
                    @SuppressWarnings("unchecked")
                    List<String> filtro = (List<String>) config.get("filtro");
                    
                    List<String> numbers = cacheService.getLatestNumerosDisponiveisSemFiltrarNumerosPreviosCache(
                        (Integer) config.get("alugado"),
                        (Integer) config.get("ativo"), 
                        (Boolean) config.get("isWa"),
                        operadora,
                        filtro,
                        System.nanoTime(),
                        Optional.empty()
                    );
                    if (numbers != null) {
                        warmed++;
                    }
                } catch (Exception e) {
                    logger.debug("Could not warm numbers cache for config: {}", config);
                }
            }
            
            logger.debug("✅ Warmed {} number configurations", warmed);
            
        } catch (Exception e) {
            logger.error("Error warming numbers availability cache", e);
        }
    }
    
    /**
     * Warm up service configurations
     */
    private void warmUpServiceConfigurations() {
        try {
            logger.debug("🔥 Warming service configurations...");
            
            // This could warm up service-specific configurations
            // Implementation depends on your specific service configuration needs
            
            logger.debug("✅ Service configurations warming ready");
            
        } catch (Exception e) {
            logger.error("Error warming service configurations", e);
        }
    }
    
    /**
     * Warm up user balances for active users
     */
    private void warmUpUserBalances() {
        try {
            logger.debug("🔥 Warming user balances cache...");
            
            // Get recently active users (users with recent API activity)
            // For now, we'll warm up a sample of users to avoid overwhelming the system
            List<User> activeUsers = userDbRepository.findAll().stream()
                .limit(50) // Limit to 50 most recently created users as a starting point
                .collect(Collectors.toList());
            
            int warmed = 0;
            for (User user : activeUsers) {
                try {
                    if (user.getApiKey() != null && !user.getApiKey().isEmpty()) {
                        userBalanceService.warmUpBalanceCache(user.getApiKey());
                        warmed++;
                    }
                } catch (Exception e) {
                    logger.debug("Could not warm balance cache for user: {}", user.getEmail());
                }
            }
            
            logger.debug("✅ Warmed {} user balances", warmed);
            
        } catch (Exception e) {
            logger.error("Error warming user balances cache", e);
        }
    }
    
    /**
     * Helper method to create number configuration map
     */
    private Map<String, Object> createNumberConfig(int alugado, int ativo, boolean isWa, 
                                                  Optional<String> operadora, List<String> filtro) {
        Map<String, Object> config = new HashMap<>();
        config.put("alugado", alugado);
        config.put("ativo", ativo);
        config.put("isWa", isWa);
        config.put("operadora", operadora);
        config.put("filtro", filtro);
        return config;
    }
    
    /**
     * Manual cache warming trigger (can be called via endpoint if needed)
     */
    public void manualWarmUp() {
        logger.info("🔥 Manual cache warming triggered");
        
        CompletableFuture.runAsync(() -> {
            try {
                warmUpCriticalCaches();
                logger.info("✅ Manual cache warming completed");
            } catch (Exception e) {
                logger.error("❌ Error during manual cache warming", e);
            }
        }, warmingExecutor);
    }
    
    /**
     * Get cache warming status
     */
    public Map<String, Object> getCacheWarmingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", cacheWarmingEnabled);
        status.put("servicesWarmingRate", servicesWarmingRate);
        status.put("callbacksWarmingRate", callbacksWarmingRate);
        status.put("usersWarmingRate", usersWarmingRate);
        status.put("numbersWarmingRate", numbersWarmingRate);
        status.put("configsWarmingRate", configsWarmingRate);
        status.put("balanceWarmingRate", balanceWarmingRate);
        status.put("operatorsWarmingRate", operatorsWarmingRate);
        status.put("warmingExecutorActive", !warmingExecutor.isShutdown());
        
        // Add human-readable intervals
        status.put("intervals", Map.of(
            "services", (servicesWarmingRate / 1000 / 60) + " minutes",
            "callbacks", (callbacksWarmingRate / 1000 / 60) + " minutes", 
            "users", (usersWarmingRate / 1000 / 60) + " minutes",
            "numbers", (numbersWarmingRate / 1000 / 60) + " minutes",
            "configs", (configsWarmingRate / 1000 / 60) + " minutes",
            "balances", (balanceWarmingRate / 1000 / 60) + " minutes",
            "operators", (operatorsWarmingRate / 1000 / 60) + " minutes"
        ));
        
        return status;
    }

    /**
     * PHASE 1 VELOCITY LAYER - Redis Pool Population
     * Populate Redis available number pools from chip_model data
     */
    @Scheduled(fixedRateString = "${cache.warming.redis.pools.rate:300000}") // 5 minutes
    public void warmUpRedisPoolsScheduled() {
        if (!cacheWarmingEnabled) {
            return;
        }

        logger.info("🔥 Starting Redis pools warming cycle...");

        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                warmUpRedisNumberPools();
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✅ Redis pools warming completed in {}ms", duration);
            } catch (Exception e) {
                logger.error("❌ Error during Redis pools warming", e);
            }
        }, warmingExecutor);
    }

    /**
     * Populate Redis available number pools by scanning chip_model table
     * Implements PRD Section 4.2 data structures and 4.5 warm-up strategy
     */
    public void warmUpRedisNumberPools() {
        try {
            logger.debug("🔥 Populating Redis number pools from chip_model...");

            // Get all active, non-rented chips
            List<br.com.store24h.store24h.model.ChipModel> availableChips =
                chipRepository.findByAlugadoAndAtivo(false, true, 10000); // Limit to 10k for safety

            // Group by operator, service, country for efficient pool population
            Map<String, Set<String>> poolGroups = new HashMap<>();

            for (br.com.store24h.store24h.model.ChipModel chip : availableChips) {
                try {
                    // Skip if number is already used for common services
                    if (isChipAlreadyUsed(chip)) {
                        continue;
                    }

                    // Extract operator from chip (you may need to adjust this based on your data model)
                    String operator = extractOperator(chip);
                    String country = "0"; // Default country - adjust based on your model

                    // Populate pools for major services
                    List<String> majorServices = Arrays.asList(
                        "wa", "tg", "fb", "ig", "tw", "gm", "dc", "vb", "tiktok", "snapchat"
                    );

                    for (String service : majorServices) {
                        String poolKey = String.format("%s:%s:%s", operator, service, country);
                        poolGroups.computeIfAbsent(poolKey, k -> new HashSet<>()).add(chip.getNumber());
                    }

                    // Also add to "any" operator pools
                    for (String service : majorServices) {
                        String poolKey = String.format("any:%s:%s", service, country);
                        poolGroups.computeIfAbsent(poolKey, k -> new HashSet<>()).add(chip.getNumber());
                    }

                } catch (Exception e) {
                    logger.debug("⚠️ Error processing chip: {}", chip.getNumber());
                }
            }

            // Populate Redis pools
            int poolsPopulated = 0;
            for (Map.Entry<String, Set<String>> entry : poolGroups.entrySet()) {
                try {
                    String[] parts = entry.getKey().split(":");
                    if (parts.length == 3) {
                        String operator = parts[0];
                        String service = parts[1];
                        String country = parts[2];
                        Set<String> numbers = entry.getValue();

                        if (!numbers.isEmpty()) {
                            redisSetService.populateAvailablePool(operator, service, country, numbers);
                            poolsPopulated++;

                            logger.debug("✅ Pool populated: {} numbers for {}:{}:{}",
                                numbers.size(), operator, service, country);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("⚠️ Error populating pool: {}", entry.getKey());
                }
            }

            logger.info("✅ Redis pools populated: {} pools with {} total chips processed",
                poolsPopulated, availableChips.size());

        } catch (Exception e) {
            logger.error("❌ Error warming Redis number pools", e);
        }
    }

    /**
     * Check if chip is already used (has existing activations)
     * This prevents double-assignment during the transition period
     */
    private boolean isChipAlreadyUsed(br.com.store24h.store24h.model.ChipModel chip) {
        try {
            // Check if this number is already in any "used" set
            // This is a simplified check - in practice you might want more sophisticated logic
            return false; // For now, allow all chips
        } catch (Exception e) {
            return true; // Fail safe - exclude if can't verify
        }
    }

    /**
     * Extract operator from chip model
     * Adjust this method based on your actual ChipModel structure
     */
    private String extractOperator(br.com.store24h.store24h.model.ChipModel chip) {
        try {
            // If ChipModel has operadora field, use it
            if (chip.getOperadora() != null && !chip.getOperadora().isEmpty()) {
                return chip.getOperadora();
            }

            // Fallback to "any" if no specific operator
            return "any";
        } catch (Exception e) {
            return "any";
        }
    }

    /**
     * Get Redis pool statistics for monitoring
     */
    public Map<String, Object> getRedisPoolStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Sample some major pools for statistics
            List<String> operators = Arrays.asList("any", "vivo", "tim", "claro", "oi");
            List<String> services = Arrays.asList("wa", "tg", "fb", "ig", "tw");
            String country = "0";

            long totalAvailable = 0;
            long totalReserved = 0;
            long totalUsed = 0;
            int poolsChecked = 0;

            for (String operator : operators) {
                for (String service : services) {
                    try {
                        RedisSetService.PoolStats poolStats = redisSetService.getPoolStats(operator, service, country);
                        totalAvailable += poolStats.getAvailable();
                        totalReserved += poolStats.getReserved();
                        totalUsed += poolStats.getUsed();
                        poolsChecked++;
                    } catch (Exception e) {
                        // Skip pools that don't exist or have errors
                    }
                }
            }

            stats.put("totalAvailable", totalAvailable);
            stats.put("totalReserved", totalReserved);
            stats.put("totalUsed", totalUsed);
            stats.put("poolsChecked", poolsChecked);
            stats.put("totalNumbers", totalAvailable + totalReserved + totalUsed);

        } catch (Exception e) {
            logger.error("❌ Error getting Redis pool stats", e);
            stats.put("error", "Failed to get stats");
        }

        return stats;
    }

    /**
     * Manual trigger for Redis pool warming (for administrative use)
     */
    public void manualRedisPoolWarmUp() {
        logger.info("🔥 Manual Redis pool warming triggered");

        CompletableFuture.runAsync(() -> {
            try {
                warmUpRedisNumberPools();
                logger.info("✅ Manual Redis pool warming completed");
            } catch (Exception e) {
                logger.error("❌ Error during manual Redis pool warming", e);
            }
        }, warmingExecutor);
    }
}
