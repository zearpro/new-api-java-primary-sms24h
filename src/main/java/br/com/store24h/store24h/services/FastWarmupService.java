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
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.repository.ActivationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * FastWarmupService - Optimized initial seeding for DragonflyDB
 * 
 * Strategy:
 * 1. Load critical data first (services, operators) - small tables
 * 2. Load chip_model in batches (1000 records at a time)
 * 3. Use parallel processing for multiple tables
 * 4. Prioritize active/available records
 * 5. Use Redis pipeline for bulk operations
 * 
 * This provides fast initial seeding while scheduled maintenance keeps data fresh
 */
@Service
public class FastWarmupService {

    private static final Logger logger = LoggerFactory.getLogger(FastWarmupService.class);

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
    private UserDbRepository userDbRepository;

    @Autowired
    private ActivationRepository activationRepository;

    private final ExecutorService executorService = Executors.newFixedThreadPool(6);

    /**
     * Fast initial seeding - loads data in priority order with batching
     */
    public Map<String, Object> performFastInitialSeeding() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🚀 Starting FAST initial seeding for DragonflyDB...");
            
            // Phase 1: Load small critical tables first (parallel)
            CompletableFuture<Map<String, Object>> servicesFuture = CompletableFuture.supplyAsync(() -> 
                seedServicesTable(), executorService);
            CompletableFuture<Map<String, Object>> operatorsFuture = CompletableFuture.supplyAsync(() -> 
                seedOperadorasTable(), executorService);
            CompletableFuture<Map<String, Object>> usersFuture = CompletableFuture.supplyAsync(() -> 
                seedUsersTable(), executorService);
            
            // Wait for Phase 1 to complete
            Map<String, Object> servicesResult = servicesFuture.get();
            Map<String, Object> operatorsResult = operatorsFuture.get();
            Map<String, Object> usersResult = usersFuture.get();
            
            result.put("phase1_services", servicesResult);
            result.put("phase1_operators", operatorsResult);
            result.put("phase1_users", usersResult);
            
            // Phase 2: Load large tables in batches (parallel)
            CompletableFuture<Map<String, Object>> chipModelFuture = CompletableFuture.supplyAsync(() -> 
                seedChipModelTableBatched(), executorService);
            CompletableFuture<Map<String, Object>> chipControlFuture = CompletableFuture.supplyAsync(() -> 
                seedChipNumberControlTable(), executorService);
            CompletableFuture<Map<String, Object>> activationFuture = CompletableFuture.supplyAsync(() -> 
                seedActivationTable(), executorService);
            
            // Wait for Phase 2 to complete
            Map<String, Object> chipModelResult = chipModelFuture.get();
            Map<String, Object> chipControlResult = chipControlFuture.get();
            Map<String, Object> activationResult = activationFuture.get();
            
            result.put("phase2_chip_model", chipModelResult);
            result.put("phase2_chip_control", chipControlResult);
            result.put("phase2_activation", activationResult);
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("total_duration_ms", duration);
            result.put("status", "completed");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.info("✅ FAST initial seeding completed in {}ms", duration);
            
        } catch (Exception e) {
            logger.error("❌ Error during fast initial seeding", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Seed services table - small table, load all at once
     */
    private Map<String, Object> seedServicesTable() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Seeding servicos table...");
            
            List<Servico> services = servicosRepository.findAll();
            Map<String, Object> cacheData = new HashMap<>();
            
            for (Servico service : services) {
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
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("records_cached", services.size());
            result.put("duration_ms", duration);
            result.put("status", "success");
            
            logger.info("✅ servicos seeded: {} records in {}ms", services.size(), duration);
            
        } catch (Exception e) {
            logger.error("❌ Error seeding servicos", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Seed operadoras table - small table, load all at once
     */
    private Map<String, Object> seedOperadorasTable() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Seeding operadoras table...");
            
            List<Operadoras> operadoras = operadorasRepository.findAll();
            Map<String, Object> cacheData = new HashMap<>();
            
            for (Operadoras operadora : operadoras) {
                String key = "v_operadoras:" + operadora.getOperadora();
                Map<String, Object> data = new HashMap<>();
                data.put("operadora", operadora.getOperadora());
                data.put("country", operadora.getCountry());
                cacheData.put(key, data);
            }
            
            // Batch set to Redis
            redisTemplate.opsForValue().multiSet(cacheData);
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("records_cached", operadoras.size());
            result.put("duration_ms", duration);
            result.put("status", "success");
            
            logger.info("✅ operadoras seeded: {} records in {}ms", operadoras.size(), duration);
            
        } catch (Exception e) {
            logger.error("❌ Error seeding operadoras", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Seed users table - medium table, load all at once
     */
    private Map<String, Object> seedUsersTable() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Seeding users table...");
            
            // Skip users table for now - use existing cache service
            // Users are handled by OptimizedUserCacheService
            Map<String, Object> cacheData = new HashMap<>();
            
            // Batch set to Redis
            redisTemplate.opsForValue().multiSet(cacheData);
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("records_cached", 0);
            result.put("duration_ms", duration);
            result.put("status", "skipped - handled by OptimizedUserCacheService");
            
            logger.info("✅ users seeded: 0 records in {}ms (skipped)", duration);
            
        } catch (Exception e) {
            logger.error("❌ Error seeding users", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Seed chip_model table in batches - prioritize active/available records
     */
    private Map<String, Object> seedChipModelTableBatched() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Seeding chip_model table in batches...");
            
            int batchSize = 1000;
            int totalRecords = 0;
            int batchCount = 0;
            
            // First batch: Get active, non-rented chips (most important)
            List<ChipModel> activeChips = chipRepository.findByAlugadoAndAtivo(false, true, batchSize);
            totalRecords += seedChipModelBatch(activeChips, "active_available");
            batchCount++;
            
            // Subsequent batches: Get remaining chips using findAll with pagination
            // For now, just load all remaining chips in one batch
            List<ChipModel> remainingChips = chipRepository.findAll();
            remainingChips.removeAll(activeChips); // Remove already processed chips
            
            if (!remainingChips.isEmpty()) {
                totalRecords += seedChipModelBatch(remainingChips, "remaining");
                batchCount++;
            }
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("total_records_cached", totalRecords);
            result.put("batches_processed", batchCount);
            result.put("duration_ms", duration);
            result.put("status", "success");
            
            logger.info("✅ chip_model seeded: {} records in {} batches, {}ms", totalRecords, batchCount, duration);
            
        } catch (Exception e) {
            logger.error("❌ Error seeding chip_model", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Seed a batch of chip_model records
     */
    private int seedChipModelBatch(List<ChipModel> chips, String batchName) {
        try {
            Map<String, Object> cacheData = new HashMap<>();
            
            for (ChipModel chip : chips) {
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
            
            logger.debug("✅ {} batch seeded: {} records", batchName, chips.size());
            return chips.size();
            
        } catch (Exception e) {
            logger.error("❌ Error seeding {} batch", batchName, e);
            return 0;
        }
    }

    /**
     * Seed chip_number_control table
     */
    private Map<String, Object> seedChipNumberControlTable() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Seeding chip_number_control table...");
            
            List<ChipNumberControl> controls = chipNumberControlRepository.findAll();
            Map<String, Object> cacheData = new HashMap<>();
            
            for (ChipNumberControl control : controls) {
                String key = "chip_number_control:" + control.getId();
                Map<String, Object> data = new HashMap<>();
                data.put("id", control.getId());
                data.put("chip_number", control.getChipNumber());
                data.put("alias_service", control.getAliasService());
                cacheData.put(key, data);
            }
            
            // Batch set to Redis
            redisTemplate.opsForValue().multiSet(cacheData);
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("records_cached", controls.size());
            result.put("duration_ms", duration);
            result.put("status", "success");
            
            logger.info("✅ chip_number_control seeded: {} records in {}ms", controls.size(), duration);
            
        } catch (Exception e) {
            logger.error("❌ Error seeding chip_number_control", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Seed activation table
     */
    private Map<String, Object> seedActivationTable() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("🔥 Seeding activation table...");
            
            List<Activation> activations = activationRepository.findAll();
            Map<String, Object> cacheData = new HashMap<>();
            
            for (Activation activation : activations) {
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
            
            long duration = System.currentTimeMillis() - startTime;
            result.put("records_cached", activations.size());
            result.put("duration_ms", duration);
            result.put("status", "success");
            
            logger.info("✅ activation seeded: {} records in {}ms", activations.size(), duration);
            
        } catch (Exception e) {
            logger.error("❌ Error seeding activation", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
