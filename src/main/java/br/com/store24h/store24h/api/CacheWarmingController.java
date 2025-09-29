package br.com.store24h.store24h.api;

import br.com.store24h.store24h.services.CacheWarmingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache Warming Controller - Provides endpoints to monitor and control cache warming
 * 
 * @author Archer (brainuxdev@gmail.com)
 */
@RestController
@RequestMapping("/api/cache")
public class CacheWarmingController {
    
    @Autowired
    private CacheWarmingService cacheWarmingService;
    
    /**
     * Get cache warming status and configuration
     */
    @GetMapping("/warming/status")
    public ResponseEntity<Map<String, Object>> getCacheWarmingStatus() {
        try {
            Map<String, Object> status = cacheWarmingService.getCacheWarmingStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get cache warming status");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Manually trigger cache warming
     */
    @PostMapping("/warming/trigger")
    public ResponseEntity<Map<String, Object>> triggerCacheWarming() {
        try {
            cacheWarmingService.manualWarmUp();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cache warming triggered successfully");
            response.put("status", "running");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to trigger cache warming");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Health check for cache warming system
     */
    @GetMapping("/warming/health")
    public ResponseEntity<Map<String, Object>> getCacheWarmingHealth() {
        try {
            Map<String, Object> status = cacheWarmingService.getCacheWarmingStatus();
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "healthy");
            health.put("cache_warming_enabled", status.get("enabled"));
            health.put("executor_active", status.get("warmingExecutorActive"));
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "unhealthy");
            error.put("error", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(error);
        }
    }
}
