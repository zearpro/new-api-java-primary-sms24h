package br.com.store24h.store24h.services;

import br.com.store24h.store24h.model.ChipModel;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.Operadoras;
import br.com.store24h.store24h.model.ChipNumberControl;
import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.repository.ChipRepository;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.repository.OperadorasRepository;
import br.com.store24h.store24h.repository.ChipNumberControlRepository;
import br.com.store24h.store24h.repository.ActivationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ProgressiveSeedingService - Timeout-resistant batch seeding with auto-reload
 * 
 * Strategy:
 * 1. Process data in small batches (500 records max per batch)
 * 2. Track progress in Redis for continuation
 * 3. Provide batch-specific endpoints for auto-reload
 * 4. Resume from last processed position
 * 5. No MySQL timeouts - small, fast operations
 */
@Service
public class ProgressiveSeedingService {

    private static final Logger logger = LoggerFactory.getLogger(ProgressiveSeedingService.class);
    
    // Batch sizes - small to avoid timeouts
    private static final int BATCH_SIZE_SMALL = 200;  // For small tables
    private static final int BATCH_SIZE_MEDIUM = 500; // For medium tables
    private static final int BATCH_SIZE_LARGE = 1000; // For large tables
    
    // Progress tracking keys
    private static final String PROGRESS_KEY_PREFIX = "seeding_progress:";
    private static final String PROGRESS_SERVICOS = PROGRESS_KEY_PREFIX + "servicos";
    private static final String PROGRESS_OPERADORAS = PROGRESS_KEY_PREFIX + "operadoras";
    private static final String PROGRESS_CHIP_MODEL = PROGRESS_KEY_PREFIX + "chip_model";
    private static final String PROGRESS_CHIP_CONTROL = PROGRESS_KEY_PREFIX + "chip_number_control";
    private static final String PROGRESS_ACTIVATION = PROGRESS_KEY_PREFIX + "activation";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ChipRepository chipRepository;

    @Autowired
    private ServicosRepository servicosRepository;

    @Autowired
    private OperadorasRepository operadorasRepository;

    @Autowired
    private ChipNumberControlRepository chipNumberControlRepository;

    @Autowired
    private ActivationRepository activationRepository;

    /**
     * Start progressive seeding - initialize progress tracking
     */
    public Map<String, Object> startProgressiveSeeding() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("🚀 Starting PROGRESSIVE seeding system...");
            
            // Initialize progress tracking
            initializeProgressTracking();
            
            result.put("status", "started");
            result.put("message", "Progressive seeding initialized. Use batch endpoints to continue.");
            result.put("batch_endpoints", List.of(
                "/api/warmup/batch/servicos",
                "/api/warmup/batch/operadoras", 
                "/api/warmup/batch/chip-model",
                "/api/warmup/batch/chip-control",
                "/api/warmup/batch/activation"
            ));
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.info("✅ Progressive seeding system initialized");
            
        } catch (Exception e) {
            logger.error("❌ Error starting progressive seeding", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Process servicos table in batches
     */
    public Map<String, Object> processServicosBatch() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Processing servicos batch...");
            
            // Get progress
            Map<String, Object> progress = getProgress(PROGRESS_SERVICOS);
            int offset = (Integer) progress.getOrDefault("offset", 0);
            int totalProcessed = (Integer) progress.getOrDefault("total_processed", 0);
            
            // Get batch of services
            List<Servico> services = servicosRepository.findAll();
            
            if (offset >= services.size()) {
                result.put("status", "completed");
                result.put("message", "All servicos processed");
                result.put("total_records", services.size());
                result.put("total_processed", totalProcessed);
                return result;
            }
            
            // Process batch
            int endIndex = Math.min(offset + BATCH_SIZE_SMALL, services.size());
            List<Servico> batch = services.subList(offset, endIndex);
            
            Map<String, Object> cacheData = new HashMap<>();
            for (Servico service : batch) {
                String key = "servicos:" + service.getId();
                Map<String, Object> data = new HashMap<>();
                data.put("id", service.getId());
                data.put("alias", service.getAlias());
                data.put("name", service.getName());
                data.put("activity", service.isActivity());
                data.put("price", service.getPrice());
                data.put("smshub", service.isSmshub() != null ? service.isSmshub() : 0);
                cacheData.put(key, data);
            }
            
            // Batch set to Redis
            redisTemplate.opsForValue().multiSet(cacheData);
            
            // Update progress
            int newOffset = endIndex;
            int newTotalProcessed = totalProcessed + batch.size();
            updateProgress(PROGRESS_SERVICOS, newOffset, newTotalProcessed, services.size());
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("status", "success");
            result.put("batch_size", batch.size());
            result.put("total_processed", newTotalProcessed);
            result.put("total_records", services.size());
            result.put("progress_percentage", (newTotalProcessed * 100) / services.size());
            result.put("duration_ms", duration);
            result.put("next_batch_available", newOffset < services.size());
            
            logger.info("✅ servicos batch processed: {}/{} records in {}ms", 
                newTotalProcessed, services.size(), duration);
            
        } catch (Exception e) {
            logger.error("❌ Error processing servicos batch", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Process operadoras table in batches
     */
    public Map<String, Object> processOperadorasBatch() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Processing operadoras batch...");
            
            // Get progress
            Map<String, Object> progress = getProgress(PROGRESS_OPERADORAS);
            int offset = (Integer) progress.getOrDefault("offset", 0);
            int totalProcessed = (Integer) progress.getOrDefault("total_processed", 0);
            
            // Get batch of operadoras
            List<Operadoras> operadoras = operadorasRepository.findAll();
            
            if (offset >= operadoras.size()) {
                result.put("status", "completed");
                result.put("message", "All operadoras processed");
                result.put("total_records", operadoras.size());
                result.put("total_processed", totalProcessed);
                return result;
            }
            
            // Process batch
            int endIndex = Math.min(offset + BATCH_SIZE_SMALL, operadoras.size());
            List<Operadoras> batch = operadoras.subList(offset, endIndex);
            
            Map<String, Object> cacheData = new HashMap<>();
            for (Operadoras operadora : batch) {
                String key = "v_operadoras:" + operadora.getOperadora();
                Map<String, Object> data = new HashMap<>();
                data.put("operadora", operadora.getOperadora());
                data.put("country", operadora.getCountry());
                cacheData.put(key, data);
            }
            
            // Batch set to Redis
            redisTemplate.opsForValue().multiSet(cacheData);
            
            // Update progress
            int newOffset = endIndex;
            int newTotalProcessed = totalProcessed + batch.size();
            updateProgress(PROGRESS_OPERADORAS, newOffset, newTotalProcessed, operadoras.size());
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("status", "success");
            result.put("batch_size", batch.size());
            result.put("total_processed", newTotalProcessed);
            result.put("total_records", operadoras.size());
            result.put("progress_percentage", (newTotalProcessed * 100) / operadoras.size());
            result.put("duration_ms", duration);
            result.put("next_batch_available", newOffset < operadoras.size());
            
            logger.info("✅ operadoras batch processed: {}/{} records in {}ms", 
                newTotalProcessed, operadoras.size(), duration);
            
        } catch (Exception e) {
            logger.error("❌ Error processing operadoras batch", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Process chip_model table in batches
     */
    public Map<String, Object> processChipModelBatch() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Processing chip_model batch...");
            
            // Get progress
            Map<String, Object> progress = getProgress(PROGRESS_CHIP_MODEL);
            int offset = (Integer) progress.getOrDefault("offset", 0);
            int totalProcessed = (Integer) progress.getOrDefault("total_processed", 0);
            
            // Get batch of chip models
            List<ChipModel> chips = chipRepository.findAll();
            
            if (offset >= chips.size()) {
                result.put("status", "completed");
                result.put("message", "All chip_model processed");
                result.put("total_records", chips.size());
                result.put("total_processed", totalProcessed);
                return result;
            }
            
            // Process batch
            int endIndex = Math.min(offset + BATCH_SIZE_MEDIUM, chips.size());
            List<ChipModel> batch = chips.subList(offset, endIndex);
            
            Map<String, Object> cacheData = new HashMap<>();
            for (ChipModel chip : batch) {
                String key = "chip_model:" + chip.getId();
                Map<String, Object> data = new HashMap<>();
                data.put("id", chip.getId());
                data.put("operadora", chip.getOperadora());
                data.put("number", chip.getNumber());
                data.put("country", chip.getCountry());
                data.put("ativo", chip.getAtivo());
                data.put("alugado", chip.getAlugado());
                data.put("pcId", chip.getPcId());
                data.put("checked", chip.getChecked());
                data.put("status", chip.getStatus());
                data.put("vendawhatsapp", chip.getVendawhatsapp());
                cacheData.put(key, data);
            }
            
            // Batch set to Redis
            redisTemplate.opsForValue().multiSet(cacheData);
            
            // Update progress
            int newOffset = endIndex;
            int newTotalProcessed = totalProcessed + batch.size();
            updateProgress(PROGRESS_CHIP_MODEL, newOffset, newTotalProcessed, chips.size());
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("status", "success");
            result.put("batch_size", batch.size());
            result.put("total_processed", newTotalProcessed);
            result.put("total_records", chips.size());
            result.put("progress_percentage", (newTotalProcessed * 100) / chips.size());
            result.put("duration_ms", duration);
            result.put("next_batch_available", newOffset < chips.size());
            
            logger.info("✅ chip_model batch processed: {}/{} records in {}ms", 
                newTotalProcessed, chips.size(), duration);
            
        } catch (Exception e) {
            logger.error("❌ Error processing chip_model batch", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Process chip_number_control table in batches
     */
    public Map<String, Object> processChipNumberControlBatch() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Processing chip_number_control batch...");
            
            // Get progress
            Map<String, Object> progress = getProgress(PROGRESS_CHIP_CONTROL);
            int offset = (Integer) progress.getOrDefault("offset", 0);
            int totalProcessed = (Integer) progress.getOrDefault("total_processed", 0);
            
            // Get batch of chip number controls
            List<ChipNumberControl> controls = chipNumberControlRepository.findAll();
            
            if (offset >= controls.size()) {
                result.put("status", "completed");
                result.put("message", "All chip_number_control processed");
                result.put("total_records", controls.size());
                result.put("total_processed", totalProcessed);
                return result;
            }
            
            // Process batch
            int endIndex = Math.min(offset + BATCH_SIZE_MEDIUM, controls.size());
            List<ChipNumberControl> batch = controls.subList(offset, endIndex);
            
            Map<String, Object> cacheData = new HashMap<>();
            for (ChipNumberControl control : batch) {
                String key = "chip_number_control:" + control.getId();
                Map<String, Object> data = new HashMap<>();
                data.put("id", control.getId());
                data.put("chip_number", control.getChipNumber());
                data.put("alias_service", control.getAliasService());
                cacheData.put(key, data);
            }
            
            // Batch set to Redis
            redisTemplate.opsForValue().multiSet(cacheData);
            
            // Update progress
            int newOffset = endIndex;
            int newTotalProcessed = totalProcessed + batch.size();
            updateProgress(PROGRESS_CHIP_CONTROL, newOffset, newTotalProcessed, controls.size());
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("status", "success");
            result.put("batch_size", batch.size());
            result.put("total_processed", newTotalProcessed);
            result.put("total_records", controls.size());
            result.put("progress_percentage", (newTotalProcessed * 100) / controls.size());
            result.put("duration_ms", duration);
            result.put("next_batch_available", newOffset < controls.size());
            
            logger.info("✅ chip_number_control batch processed: {}/{} records in {}ms", 
                newTotalProcessed, controls.size(), duration);
            
        } catch (Exception e) {
            logger.error("❌ Error processing chip_number_control batch", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Process activation table in batches
     */
    public Map<String, Object> processActivationBatch() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Processing activation batch...");
            
            // Get progress
            Map<String, Object> progress = getProgress(PROGRESS_ACTIVATION);
            int offset = (Integer) progress.getOrDefault("offset", 0);
            int totalProcessed = (Integer) progress.getOrDefault("total_processed", 0);
            
            // Get batch of activations
            List<Activation> activations = activationRepository.findAll();
            
            if (offset >= activations.size()) {
                result.put("status", "completed");
                result.put("message", "All activation processed");
                result.put("total_records", activations.size());
                result.put("total_processed", totalProcessed);
                return result;
            }
            
            // Process batch
            int endIndex = Math.min(offset + BATCH_SIZE_MEDIUM, activations.size());
            List<Activation> batch = activations.subList(offset, endIndex);
            
            Map<String, Object> cacheData = new HashMap<>();
            for (Activation activation : batch) {
                String key = "activation:" + activation.getId();
                Map<String, Object> data = new HashMap<>();
                data.put("id", activation.getId());
                data.put("chip_number", activation.getChipNumber());
                data.put("api_key", activation.getApiKey());
                data.put("status", activation.getStatus());
                data.put("initial_time", activation.getInitialTime());
                cacheData.put(key, data);
            }
            
            // Batch set to Redis
            redisTemplate.opsForValue().multiSet(cacheData);
            
            // Update progress
            int newOffset = endIndex;
            int newTotalProcessed = totalProcessed + batch.size();
            updateProgress(PROGRESS_ACTIVATION, newOffset, newTotalProcessed, activations.size());
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("status", "success");
            result.put("batch_size", batch.size());
            result.put("total_processed", newTotalProcessed);
            result.put("total_records", activations.size());
            result.put("progress_percentage", (newTotalProcessed * 100) / activations.size());
            result.put("duration_ms", duration);
            result.put("next_batch_available", newOffset < activations.size());
            
            logger.info("✅ activation batch processed: {}/{} records in {}ms", 
                newTotalProcessed, activations.size(), duration);
            
        } catch (Exception e) {
            logger.error("❌ Error processing activation batch", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Get overall seeding progress
     */
    public Map<String, Object> getOverallProgress() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> servicosProgress = getProgress(PROGRESS_SERVICOS);
            Map<String, Object> operadorasProgress = getProgress(PROGRESS_OPERADORAS);
            Map<String, Object> chipModelProgress = getProgress(PROGRESS_CHIP_MODEL);
            Map<String, Object> chipControlProgress = getProgress(PROGRESS_CHIP_CONTROL);
            Map<String, Object> activationProgress = getProgress(PROGRESS_ACTIVATION);
            
            result.put("servicos", servicosProgress);
            result.put("operadoras", operadorasProgress);
            result.put("chip_model", chipModelProgress);
            result.put("chip_number_control", chipControlProgress);
            result.put("activation", activationProgress);
            
            // Calculate overall progress
            int totalProcessed = (Integer) servicosProgress.getOrDefault("total_processed", 0) +
                               (Integer) operadorasProgress.getOrDefault("total_processed", 0) +
                               (Integer) chipModelProgress.getOrDefault("total_processed", 0) +
                               (Integer) chipControlProgress.getOrDefault("total_processed", 0) +
                               (Integer) activationProgress.getOrDefault("total_processed", 0);
            
            int totalRecords = (Integer) servicosProgress.getOrDefault("total_records", 0) +
                             (Integer) operadorasProgress.getOrDefault("total_records", 0) +
                             (Integer) chipModelProgress.getOrDefault("total_records", 0) +
                             (Integer) chipControlProgress.getOrDefault("total_records", 0) +
                             (Integer) activationProgress.getOrDefault("total_records", 0);
            
            int overallPercentage = totalRecords > 0 ? (totalProcessed * 100) / totalRecords : 0;
            
            result.put("overall_progress", Map.of(
                "total_processed", totalProcessed,
                "total_records", totalRecords,
                "percentage", overallPercentage
            ));
            
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            logger.error("❌ Error getting overall progress", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Reset all progress (start over)
     */
    public Map<String, Object> resetProgress() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("🔄 Resetting all seeding progress...");
            
            redisTemplate.delete(PROGRESS_SERVICOS);
            redisTemplate.delete(PROGRESS_OPERADORAS);
            redisTemplate.delete(PROGRESS_CHIP_MODEL);
            redisTemplate.delete(PROGRESS_CHIP_CONTROL);
            redisTemplate.delete(PROGRESS_ACTIVATION);
            
            result.put("status", "success");
            result.put("message", "All progress reset. Ready to start fresh.");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.info("✅ All seeding progress reset");
            
        } catch (Exception e) {
            logger.error("❌ Error resetting progress", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    // Helper methods

    private void initializeProgressTracking() {
        // Initialize progress for each table
        updateProgress(PROGRESS_SERVICOS, 0, 0, 0);
        updateProgress(PROGRESS_OPERADORAS, 0, 0, 0);
        updateProgress(PROGRESS_CHIP_MODEL, 0, 0, 0);
        updateProgress(PROGRESS_CHIP_CONTROL, 0, 0, 0);
        updateProgress(PROGRESS_ACTIVATION, 0, 0, 0);
    }

    private void updateProgress(String progressKey, int offset, int totalProcessed, int totalRecords) {
        Map<String, Object> progress = new HashMap<>();
        progress.put("offset", offset);
        progress.put("total_processed", totalProcessed);
        progress.put("total_records", totalRecords);
        progress.put("last_updated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        redisTemplate.opsForValue().set(progressKey, progress);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getProgress(String progressKey) {
        Object progressObj = redisTemplate.opsForValue().get(progressKey);
        if (progressObj instanceof Map) {
            return (Map<String, Object>) progressObj;
        }
        return new HashMap<>();
    }
}
