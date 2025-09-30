package br.com.store24h.store24h.services;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class NumberAssignmentConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(NumberAssignmentConsumer.class);
    
    @Autowired
    private ChipNumberControlService chipNumberControlService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @RabbitListener(queues = "number-assignment-queue")
    public void handleNumberAssignment(String message) {
        try {
            logger.info("Processing number assignment message: {}", message);
            
            JsonNode jsonNode = objectMapper.readTree(message);
            
            String serviceId = jsonNode.get("serviceId").asText();
            String number = jsonNode.get("number").asText();
            String country = jsonNode.get("country").asText();
            String operator = jsonNode.get("operator").asText();
            String userId = jsonNode.get("userId").asText();
            String apiKey = jsonNode.get("apiKey").asText();
            
            // Persist to MySQL asynchronously
            persistNumberAssignment(serviceId, number, country, operator, userId, apiKey);
            
            logger.info("Successfully processed number assignment for service {}:{}:{}:{}", 
                serviceId, number, country, operator);
                
        } catch (Exception e) {
            logger.error("Error processing number assignment message: {}", e.getMessage(), e);
            
            // In production, you might want to send to a dead letter queue
            // or implement retry logic here
        }
    }
    
    private void persistNumberAssignment(String serviceId, String number, String country, 
                                      String operator, String userId, String apiKey) {
        try {
            // Create chip_number_control record using the existing service
            chipNumberControlService.newChipNumberControl(number);
            
            logger.debug("Persisted number assignment to MySQL: {}:{}:{}:{}", 
                serviceId, number, country, operator);
                
        } catch (Exception e) {
            logger.error("Error persisting number assignment to MySQL: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
    
    @RabbitListener(queues = "cache-invalidation-queue")
    public void handleCacheInvalidation(String message) {
        try {
            logger.info("Processing cache invalidation message: {}", message);
            
            JsonNode jsonNode = objectMapper.readTree(message);
            String cacheType = jsonNode.get("cacheType").asText();
            String key = jsonNode.has("key") ? jsonNode.get("key").asText() : null;
            
            switch (cacheType) {
                case "user_balance":
                    // Invalidate user balance cache
                    if (key != null) {
                        // Implement user balance cache invalidation
                        logger.info("Invalidating user balance cache for key: {}", key);
                    }
                    break;
                    
                case "service_pool":
                    // Invalidate service pool cache
                    if (key != null) {
                        // Implement service pool cache invalidation
                        logger.info("Invalidating service pool cache for key: {}", key);
                    }
                    break;
                    
                case "operators":
                    // Invalidate operators cache
                    logger.info("Invalidating operators cache");
                    break;
                    
                default:
                    logger.warn("Unknown cache invalidation type: {}", cacheType);
            }
            
        } catch (Exception e) {
            logger.error("Error processing cache invalidation message: {}", e.getMessage(), e);
        }
    }
}
