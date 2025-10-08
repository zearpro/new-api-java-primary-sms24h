package br.com.store24h.store24h.services;

import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.ActivationRepository;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.services.core.ActivationService;
import br.com.store24h.store24h.services.ChipNumberControlService;
import br.com.store24h.store24h.MongoService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * NumberAssignConsumer - RabbitMQ consumer for async number assignment processing
 *
 * Implements Phase 1 Velocity Layer messaging pattern:
 * 1. getNumber returns immediately with reserved number
 * 2. This consumer processes the assignment async:
 *    - Persists activation in MySQL
 *    - Finalizes Redis state (move from reserved to used)
 *    - Updates MongoDB if required
 *    - Handles rollback on failures
 *
 * Queue: number.assigned
 * Message format: {userId, apiKey, operator, service, country, number, activationId?, reservationToken}
 *
 * @author PRD Implementation - Velocity Layer
 */
@Service
public class NumberAssignConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NumberAssignConsumer.class);

    @Autowired
    private RedisSetService redisSetService;

    @Autowired
    private ActivationService activationService;

    @Autowired
    private ChipNumberControlService chipNumberControlService;

    @Autowired
    private UserDbRepository userDbRepository;

    @Autowired
    private ServicosRepository servicosRepository;

    @Autowired
    private ActivationRepository activationRepository;

    @Autowired
    private MongoService mongoService;

    private final Gson gson = new Gson();

    /**
     * Process number assignment messages from RabbitMQ
     */
    @RabbitListener(queues = "number.assigned")
    @Transactional
    public void processNumberAssignment(String message) {
        long startTime = System.nanoTime();
        NumberAssignmentMessage msg = null;

        try {
            // Parse message
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
            msg = parseAssignmentMessage(jsonMessage);

            logger.debug("🔄 Processing number assignment: {} for service {} (token: {})",
                msg.number, msg.service, msg.reservationToken.substring(0, 8) + "***");

            // Process assignment with comprehensive error handling
            boolean success = processAssignment(msg);

            long duration = (System.nanoTime() - startTime) / 1000000;

            if (success) {
                logger.info("✅ Number assignment completed: {} for service {} in {}ms",
                    msg.number, msg.service, duration);
            } else {
                logger.error("❌ Number assignment failed: {} for service {} in {}ms",
                    msg.number, msg.service, duration);
            }

        } catch (Exception e) {
            long duration = (System.nanoTime() - startTime) / 1000000;
            logger.error("❌ Fatal error processing number assignment in {}ms", duration, e);

            // Attempt rollback if we have enough info
            if (msg != null) {
                attemptEmergencyRollback(msg);
            }
        }
    }

    /**
     * Process the number assignment with full database persistence and Redis finalization
     */
    private boolean processAssignment(NumberAssignmentMessage msg) {
        try {
            // Step 1: Validate user still exists
            Optional<User> userOpt = userDbRepository.findByApiKey(msg.apiKey);
            if (!userOpt.isPresent()) {
                logger.error("❌ User not found for API key during assignment: {}",
                    msg.apiKey.substring(0, 8) + "***");
                rollbackReservation(msg);
                return false;
            }

            // Step 2: Validate service still exists and is active
            Optional<Servico> servicoOpt = servicosRepository.findFirstByAlias(msg.service);
            if (!servicoOpt.isPresent() || !servicoOpt.get().isActivity()) {
                logger.error("❌ Service {} not found or inactive during assignment", msg.service);
                rollbackReservation(msg);
                return false;
            }

            User user = userOpt.get();
            Servico servico = servicoOpt.get();

            // Step 3: Create activation in database
            Activation activation;
            try {
                activation = activationService.newActivation(user, servico, msg.number, msg.apiKey, 2);
                logger.debug("✅ Activation created: ID {} for number {}", activation.getId(), msg.number);
            } catch (Exception e) {
                logger.error("❌ Failed to create activation for number {}", msg.number, e);
                rollbackReservation(msg);
                return false;
            }

            // Step 4: Confirm reservation in Redis (move from reserved to used)
            boolean confirmed = redisSetService.confirmReservation(
                msg.operator, msg.service, msg.country, msg.reservationToken, msg.number
            );

            if (!confirmed) {
                logger.error("❌ Failed to confirm Redis reservation for number {}", msg.number);
                // Try to cancel the activation we just created
                try {
                    activationService.cancelActivation(activation, msg.apiKey, 2);
                } catch (Exception cancelException) {
                    logger.error("❌ Failed to cancel activation during rollback", cancelException);
                }
                return false;
            }

            // Step 5: Add service to number control SYNCHRONOUSLY to prevent race conditions
            // This ensures the number is marked as taken before any other operations
            try {
                chipNumberControlService.addServiceInNumber(msg.number, servico);
                logger.debug("✅ Service {} added to number {}", msg.service, msg.number);
            } catch (Exception e) {
                logger.error("❌ Failed to add service to number control for {}", msg.number, e);
                // If we can't mark the number as taken, we should rollback
                rollbackReservation(msg);
                return false;
            }

            // Step 6: Update activation state in Redis for fast polling
            redisSetService.setActivationState(activation.getId(), msg.number, msg.service, "STATUS_WAIT_CODE");

            // Step 7: Mirror to MongoDB if required (async, non-blocking)
            if (shouldMirrorToMongo(msg.service)) {
                CompletableFuture.runAsync(() -> {
                    try {
                        mirrorToMongoDB(activation, msg);
                        logger.debug("✅ Activation mirrored to MongoDB: {}", activation.getId());
                    } catch (Exception e) {
                        logger.error("❌ Failed to mirror activation {} to MongoDB", activation.getId(), e);
                    }
                });
            }

            return true;

        } catch (Exception e) {
            logger.error("❌ Unexpected error during assignment processing", e);
            rollbackReservation(msg);
            return false;
        }
    }

    /**
     * Rollback reservation to available pool
     */
    private void rollbackReservation(NumberAssignmentMessage msg) {
        try {
            boolean rolledBack = redisSetService.rollbackReservation(
                msg.operator, msg.service, msg.country, msg.reservationToken, msg.number
            );

            if (rolledBack) {
                logger.debug("✅ Reservation rolled back for number {}", msg.number);
            } else {
                logger.warn("⚠️ Failed to rollback reservation for number {}", msg.number);
            }
        } catch (Exception e) {
            logger.error("❌ Error during reservation rollback for number {}", msg.number, e);
        }
    }

    /**
     * Emergency rollback when we have minimal information
     */
    private void attemptEmergencyRollback(NumberAssignmentMessage msg) {
        logger.warn("🚨 Attempting emergency rollback for message");
        try {
            if (msg.reservationToken != null && msg.number != null) {
                rollbackReservation(msg);
            }
        } catch (Exception e) {
            logger.error("❌ Emergency rollback failed", e);
        }
    }

    /**
     * Parse assignment message from JSON
     */
    private NumberAssignmentMessage parseAssignmentMessage(JsonObject json) {
        NumberAssignmentMessage msg = new NumberAssignmentMessage();

        msg.userId = json.has("userId") ? json.get("userId").getAsLong() : null;
        msg.apiKey = json.get("apiKey").getAsString();
        msg.operator = json.get("operator").getAsString();
        msg.service = json.get("service").getAsString();
        msg.country = json.get("country").getAsString();
        msg.number = json.get("number").getAsString();
        msg.reservationToken = json.get("reservationToken").getAsString();
        msg.activationId = json.has("activationId") ? json.get("activationId").getAsLong() : null;
        msg.timestamp = json.has("timestamp") ? json.get("timestamp").getAsLong() : System.currentTimeMillis();

        return msg;
    }

    /**
     * Determine if activation should be mirrored to MongoDB
     */
    private boolean shouldMirrorToMongo(String service) {
        // During migration period, mirror everything except 'ot' service
        return !service.equals("ot");
    }

    /**
     * Mirror activation to MongoDB for legacy compatibility
     */
    private void mirrorToMongoDB(Activation activation, NumberAssignmentMessage msg) {
        try {
            // Add to numbers_to_callback collection with TTL
            Map<String, Object> callbackData = Map.of(
                "activationId", activation.getId(),
                "numero", activation.getChipNumber(),
                "servico", activation.getAliasService(),
                "createdAt", new java.util.Date()
            );
            mongoService.insert(callbackData);

            // Add to compras_ativadas using existing method
            mongoService.bloqueiaNumeroServico(
                msg.apiKey,
                String.valueOf(activation.getId()),
                activation.getChipNumber(),
                activation.getAliasService()
            );

        } catch (Exception e) {
            logger.error("❌ Failed to mirror to MongoDB", e);
            throw e;
        }
    }

    /**
     * Get consumer statistics for monitoring
     */
    public ConsumerStats getConsumerStats() {
        // This would typically track metrics like:
        // - Messages processed per second
        // - Success/failure rates
        // - Average processing time
        // - Queue depth

        return new ConsumerStats(
            0L, // messages processed (implement counter)
            0L, // successful assignments (implement counter)
            0L, // failed assignments (implement counter)
            0L  // rollbacks (implement counter)
        );
    }

    /**
     * Message structure for number assignments
     */
    private static class NumberAssignmentMessage {
        Long userId;
        String apiKey;
        String operator;
        String service;
        String country;
        String number;
        String reservationToken;
        Long activationId;
        Long timestamp;
    }

    /**
     * Consumer statistics for monitoring
     */
    public static class ConsumerStats {
        private final long messagesProcessed;
        private final long successfulAssignments;
        private final long failedAssignments;
        private final long rollbacks;

        public ConsumerStats(long messagesProcessed, long successfulAssignments,
                           long failedAssignments, long rollbacks) {
            this.messagesProcessed = messagesProcessed;
            this.successfulAssignments = successfulAssignments;
            this.failedAssignments = failedAssignments;
            this.rollbacks = rollbacks;
        }

        public long getMessagesProcessed() { return messagesProcessed; }
        public long getSuccessfulAssignments() { return successfulAssignments; }
        public long getFailedAssignments() { return failedAssignments; }
        public long getRollbacks() { return rollbacks; }
        public double getSuccessRate() {
            return messagesProcessed > 0 ? (double) successfulAssignments / messagesProcessed : 0.0;
        }
    }
}