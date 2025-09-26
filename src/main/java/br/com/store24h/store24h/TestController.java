package br.com.store24h.store24h;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "✅ API is running");
        response.put("message", "Store24h API - Port 80 Working");
        response.put("timestamp", System.currentTimeMillis());
        response.put("endpoints", Map.of(
            "health", "/health or /actuator/health", 
            "docs", "/docs/",
            "api-docs", "/api-docs"
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/internal/running")
    public ResponseEntity<Map<String, String>> internalHealth() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("port", "80");
        response.put("service", "Store24h API");
        return ResponseEntity.ok(response);
    }
}
