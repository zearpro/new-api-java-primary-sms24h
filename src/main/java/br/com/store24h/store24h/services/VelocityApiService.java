package br.com.store24h.store24h.services;

import br.com.store24h.store24h.apiv2.TipoDeApiEnum;
import br.com.store24h.store24h.apiv2.exceptions.ApiKeyNotFoundException;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.ChipModel;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.repository.ChipRepository;
import br.com.store24h.store24h.services.core.TipoDeApiNotPermitedException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * VelocityApiService - Phase 1 implementation of high-performance getNumber and getPrices
 *
 * Key Performance Features:
 * - Redis-first number reservation (atomic with Lua scripts)
 * - Immediate response (< 30ms target)
 * - Async persistence via RabbitMQ
 * - Comprehensive error handling with rollbacks
 * - Performance metrics and monitoring
 *
 * Algorithm (getNumber):
 * 1. Validate API key (cached)
 * 2. Atomic reserve from Redis pool
 * 3. Return number immediately
 * 4. Publish assignment event to RabbitMQ
 * 5. Consumer handles DB persistence + Redis finalization
 *
 * @author PRD Implementation - Phase 1 Velocity Layer
 */
@Service
public class VelocityApiService {

    private static final Logger logger = LoggerFactory.getLogger(VelocityApiService.class);

    @Autowired
    private RedisSetService redisSetService;

    @Autowired
    private OptimizedPublicApiService optimizedApiService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ServicosRepository servicosRepository;

    @Autowired
    private ChipRepository chipRepository;

    // Performance metrics
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private Timer getNumberTimer;
    private Timer getPricesTimer;
    private Counter reservationSuccessCounter;
    private Counter reservationFailCounter;
    private Counter cacheHitCounter;
    private Counter cacheMissCounter;

    private final Gson gson = new Gson();

    public VelocityApiService() {
        // Initialize metrics if meter registry is available
        initializeMetrics();
    }

    /**
     * High-performance getNumber with Redis-first reservation
     * Target: P50 <= 30ms, P95 <= 60ms
     */
    public String getNumberVelocity(String apiKey, Optional<String> service, Optional<String> operator,
                                   Optional<String> country, Optional<String> numero, int version,
                                   TipoDeApiEnum tipoDeApiEnum) throws ApiKeyNotFoundException, TipoDeApiNotPermitedException {

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.nanoTime();

        try {
            // Step 1: Fast validation
            if (!service.isPresent() || !country.isPresent()) {
                return "BAD_ACTION";
            }

            String serviceAlias = service.get();
            String operatorName = operator.orElse("any");
            String countryCode = country.get();

            // Step 2: Validate user with optimized caching
            User user = optimizedApiService.validateUserOptimized(apiKey);
            optimizedApiService.validateApiTypeOptimized(apiKey, tipoDeApiEnum);

            // Step 3: Validate service
            Optional<Servico> servicoOpt = optimizedApiService.getServiceOptimized(serviceAlias);
            if (!servicoOpt.isPresent() || !servicoOpt.get().isActivity()) {
                return "BAD_SERVICE";
            }

            Servico servico = servicoOpt.get();

            // Step 4: Check balance
            if (!optimizedApiService.hasValidBalanceOptimized(apiKey, servico.getPrice())) {
                return "NO_BALANCE";
            }

            // Step 5: Special WhatsApp validation
            if (serviceAlias.equals("wa") && !optimizedApiService.isWhatsAppEnabledOptimized(apiKey)) {
                return "FORBIDEN_SERVICE";
            }

            // Step 6: Validate operator
            if (!optimizedApiService.isValidOperatorCached(operatorName)) {
                return "INVALID_OPERATOR";
            }

            // Step 7: Atomic number reservation from Redis
            RedisSetService.ReservationResult reservation = redisSetService.reserveNumber(
                operatorName, serviceAlias, countryCode
            );

            if (!reservation.isSuccess()) {
                incrementCounter(reservationFailCounter);

                if ("NO_NUMBERS".equals(reservation.getError())) {
                    // Try to warm up the pool and retry once
                    warmUpPoolAndRetry(operatorName, serviceAlias, countryCode);
                    reservation = redisSetService.reserveNumber(operatorName, serviceAlias, countryCode);

                    if (!reservation.isSuccess()) {
                        return "NO_NUMBERS";
                    }
                } else {
                    return "ERROR_REDIS";
                }
            }

            incrementCounter(reservationSuccessCounter);

            // Step 8: Generate activation ID (temporary, will be replaced by consumer)
            long tempActivationId = System.currentTimeMillis();

            // Step 9: Publish assignment message to RabbitMQ
            publishAssignmentMessage(user, servico, reservation, operatorName, serviceAlias, countryCode, apiKey, version);

            // Step 10: Return immediate response
            String response = String.format("ACCESS_NUMBER:%d:%s", tempActivationId, reservation.getNumber());

            long duration = (System.nanoTime() - startTime) / 1000000;
            logger.info("✅ getNumber completed in {}ms - Number: {} Service: {} Operator: {}",
                duration, reservation.getNumber(), serviceAlias, operatorName);

            return response;

        } catch (ApiKeyNotFoundException | TipoDeApiNotPermitedException e) {
            throw e; // Re-throw validation exceptions
        } catch (Exception e) {
            logger.error("❌ Unexpected error in getNumberVelocity", e);
            return "ERROR_SQL";
        } finally {
            if (getNumberTimer != null) {
                sample.stop(getNumberTimer);
            }
        }
    }

    /**
     * Optimized getPrices with full Redis caching
     * Uses existing OptimizedPublicApiService implementation
     */
    public Object getPricesVelocity(Optional<String> service, Optional<String> country, Optional<String> operator) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            incrementCounter(cacheHitCounter); // Assume cache hit for optimized service
            return optimizedApiService.getPricesOptimized(service, country, operator);

        } catch (Exception e) {
            incrementCounter(cacheMissCounter);
            logger.error("❌ Error in getPricesVelocity", e);

            // Fallback to database
            return fallbackGetPrices(service, country);
        } finally {
            if (getPricesTimer != null) {
                sample.stop(getPricesTimer);
            }
        }
    }

    /**
     * Publish number assignment message to RabbitMQ
     */
    private void publishAssignmentMessage(User user, Servico servico, RedisSetService.ReservationResult reservation,
                                         String operator, String service, String country, String apiKey, int version) {
        try {
            JsonObject message = new JsonObject();
            message.addProperty("userId", user.getId());
            message.addProperty("apiKey", apiKey);
            message.addProperty("operator", operator);
            message.addProperty("service", service);
            message.addProperty("country", country);
            message.addProperty("number", reservation.getNumber());
            message.addProperty("reservationToken", reservation.getToken());
            message.addProperty("version", version);
            message.addProperty("timestamp", System.currentTimeMillis());

            String messageJson = gson.toJson(message);

            rabbitTemplate.convertAndSend("number.assigned", messageJson);

            logger.debug("✅ Assignment message published for number: {}", reservation.getNumber());

        } catch (Exception e) {
            logger.error("❌ Failed to publish assignment message", e);
            // Attempt rollback
            try {
                redisSetService.rollbackReservation(operator, service, country,
                    reservation.getToken(), reservation.getNumber());
                logger.warn("⚠️ Rolled back reservation due to message publication failure");
            } catch (Exception rollbackException) {
                logger.error("❌ Failed to rollback after message publication failure", rollbackException);
            }
            throw e;
        }
    }

    /**
     * Attempt to warm up pool and retry reservation
     */
    private void warmUpPoolAndRetry(String operator, String service, String country) {
        try {
            logger.debug("🔥 Attempting pool warm-up for {}:{}:{}", operator, service, country);

            // Fetch a small batch of candidate numbers from DB to seed Redis pool
            // Prefer operator+country filtered queries; fallback to country-only
            java.util.List<ChipModel> candidates;
            if (operator != null && !operator.equalsIgnoreCase("any")) {
                try {
                    candidates = chipRepository.findByCountryAndAlugadoAndAtivoAndOperadora(country, false, true, operator);
                } catch (Exception ex) {
                    candidates = chipRepository.findByCountryAndAlugadoAndAtivo(country, false, true);
                }
            } else {
                candidates = chipRepository.findByCountryAndAlugadoAndAtivo(country, false, true);
            }

            java.util.Set<String> numbers = new java.util.HashSet<>();
            int limit = Math.min(candidates.size(), 200);
            for (int i = 0; i < limit; i++) {
                numbers.add(candidates.get(i).getNumber());
            }

            if (!numbers.isEmpty()) {
                redisSetService.populateAvailablePool(operator, service, country, numbers);
                logger.debug("✅ Warmed {} numbers for pool {}:{}:{}", numbers.size(), operator, service, country);
            } else {
                logger.debug("⚠️ No candidates found to warm pool {}:{}:{}", operator, service, country);
            }

        } catch (Exception e) {
            logger.debug("⚠️ Pool warm-up failed for {}:{}:{}", operator, service, country, e);
        }
    }

    /**
     * Fallback getPrices implementation
     */
    private Object fallbackGetPrices(Optional<String> service, Optional<String> country) {
        try {
            if (service.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                Optional<Servico> servicoOpt = servicosRepository.findFirstByAlias(service.get());
                if (servicoOpt.isPresent()) {
                    Servico s = servicoOpt.get();
                    Map<String, Object> serviceData = new HashMap<>();
                    Map<String, Object> priceData = new HashMap<>();
                    priceData.put("cost", s.getPrice());
                    priceData.put("count", s.getTotalQuantity());
                    serviceData.put(s.getAlias(), priceData);
                    result.put(country.get(), serviceData);
                }
                return result;
            }

            // All services fallback
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> serviceData = new HashMap<>();

            servicosRepository.findAll().forEach(s -> {
                Map<String, Object> priceData = new HashMap<>();
                priceData.put("cost", s.getPrice());
                priceData.put("count", s.getTotalQuantity());
                serviceData.put(s.getAlias(), priceData);
            });

            result.put(country.get(), serviceData);
            return result;

        } catch (Exception e) {
            logger.error("❌ Fallback getPrices failed", e);
            return Map.of("error", "Service unavailable");
        }
    }

    /**
     * Initialize performance metrics
     */
    private void initializeMetrics() {
        if (meterRegistry != null) {
            getNumberTimer = Timer.builder("velocity.getNumber.duration")
                .description("Duration of getNumber calls")
                .register(meterRegistry);

            getPricesTimer = Timer.builder("velocity.getPrices.duration")
                .description("Duration of getPrices calls")
                .register(meterRegistry);

            reservationSuccessCounter = Counter.builder("velocity.reservations.success")
                .description("Successful number reservations")
                .register(meterRegistry);

            reservationFailCounter = Counter.builder("velocity.reservations.failed")
                .description("Failed number reservations")
                .register(meterRegistry);

            cacheHitCounter = Counter.builder("velocity.cache.hits")
                .description("Cache hits")
                .register(meterRegistry);

            cacheMissCounter = Counter.builder("velocity.cache.misses")
                .description("Cache misses")
                .register(meterRegistry);
        }
    }

    /**
     * Helper method to safely increment counters
     */
    private void incrementCounter(Counter counter) {
        if (counter != null) {
            counter.increment();
        }
    }

    /**
     * Get performance statistics
     */
    public Map<String, Object> getVelocityStats() {
        Map<String, Object> stats = new HashMap<>();

        if (meterRegistry != null) {
            stats.put("getNumberAvgDuration", getNumberTimer.mean(TimeUnit.MILLISECONDS));
            stats.put("getPricesAvgDuration", getPricesTimer.mean(TimeUnit.MILLISECONDS));
            stats.put("reservationSuccessCount", reservationSuccessCounter.count());
            stats.put("reservationFailCount", reservationFailCounter.count());
            stats.put("cacheHitCount", cacheHitCounter.count());
            stats.put("cacheMissCount", cacheMissCounter.count());

            // Calculate success rate
            double totalReservations = reservationSuccessCounter.count() + reservationFailCounter.count();
            double successRate = totalReservations > 0 ? (reservationSuccessCounter.count() / totalReservations) * 100 : 0;
            stats.put("reservationSuccessRate", String.format("%.2f%%", successRate));
        } else {
            stats.put("metricsEnabled", false);
        }

        return stats;
    }

    /**
     * Check if velocity layer is healthy
     */
    public boolean isVelocityHealthy() {
        try {
            // Check Redis connectivity
            redisSetService.getPoolStats("any", "test", "0");

            // Check RabbitMQ connectivity
            rabbitTemplate.convertAndSend("health.check", "ping");

            return true;
        } catch (Exception e) {
            logger.error("❌ Velocity layer health check failed", e);
            return false;
        }
    }
}