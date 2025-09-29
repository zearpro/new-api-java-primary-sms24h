/*
 * Country and Operator validation service with Redis caching
 */
package br.com.store24h.store24h.services;

import br.com.store24h.store24h.RedisService;
import br.com.store24h.store24h.model.Operadoras;
import br.com.store24h.store24h.repository.OperadorasRepository;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CountryOperatoraCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CountryOperatoraCacheService.class);
    
    @Autowired
    private OperadorasRepository operadorasRepository;
    
    @Autowired
    private RedisService redisService;
    
    private static final String CACHE_KEY = "country_operadora_valid";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    /**
     * Check if country+operator combination is valid
     */
    @Cacheable(value = "countryOperadoraValidation", key = "#country + ':' + #operator")
    public boolean isValidCombination(String country, String operator) {
        try {
            if (operator.equalsIgnoreCase("any")) {
                // For "any" operator, just check if country exists
                return isValidCountry(country);
            }
            
            // Check specific country+operator combination
            List<Operadoras> combinations = operadorasRepository.findAll();
            boolean isValid = combinations.stream()
                .anyMatch(op -> op.getCountry() != null && 
                               country.equals(op.getCountry()) && 
                               operator.equalsIgnoreCase(op.getOperadora()));
            
            // ✅ Fallback: If no country data exists yet, allow existing operators
            if (!isValid && combinations.stream().anyMatch(op -> operator.equalsIgnoreCase(op.getOperadora()))) {
                logger.warn("Country validation failed but operator {} exists - allowing during migration", operator);
                isValid = true;
            }
            
            logger.debug("Validation result for {}:{} = {}", country, operator, isValid);
            return isValid;
                               
        } catch (Exception e) {
            logger.error("Error validating {}:{}", country, operator, e);
            return true; // Fail-safe: allow if can't verify
        }
    }
    
    /**
     * Check if country exists in v_operadoras
     */
    public boolean isValidCountry(String country) {
        try {
            List<Operadoras> all = operadorasRepository.findAll();
            boolean isValid = all.stream().anyMatch(op -> op.getCountry() != null && country.equals(op.getCountry()));
            
            // ✅ Fallback: During migration, if no country data exists, allow common countries
            if (!isValid && (country.equals("73") || country.equals("36"))) {
                logger.warn("Country validation failed for {} - allowing during migration", country);
                isValid = true;
            }
            
            logger.debug("Country validation for {} = {}", country, isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating country: {}", country, e);
            return true; // Fail-safe
        }
    }
    
    /**
     * Get all valid operators for a specific country
     */
    public List<String> getOperatorsForCountry(String country) {
        try {
            List<Operadoras> all = operadorasRepository.findAll();
            List<String> operators = all.stream()
                .filter(op -> country.equals(op.getCountry()))
                .map(Operadoras::getOperadora)
                .distinct()
                .collect(Collectors.toList());
                
            logger.debug("Found {} operators for country {}", operators.size(), country);
            return operators;
        } catch (Exception e) {
            logger.error("Error getting operators for country: {}", country, e);
            return List.of("tim", "vivo", "claro"); // Fallback
        }
    }
    
    /**
     * Get all valid countries
     */
    public List<String> getAllValidCountries() {
        try {
            List<Operadoras> all = operadorasRepository.findAll();
            List<String> countries = all.stream()
                .map(Operadoras::getCountry)
                .distinct()
                .collect(Collectors.toList());
                
            logger.debug("Found {} valid countries", countries.size());
            return countries;
        } catch (Exception e) {
            logger.error("Error getting valid countries", e);
            return List.of("73", "36"); // Fallback
        }
    }
    
    /**
     * Warm up the cache
     */
    public void warmUpCache() {
        try {
            logger.info("🔥 Warming up country+operator validation cache...");
            
            List<Operadoras> all = operadorasRepository.findAll();
            
            // Pre-validate common combinations
            for (Operadoras op : all) {
                isValidCombination(op.getCountry(), op.getOperadora());
                isValidCountry(op.getCountry());
            }
            
            logger.info("✅ Cache warmed up with {} combinations", all.size());
        } catch (Exception e) {
            logger.error("❌ Error warming up cache", e);
        }
    }
    
    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        return "CountryOperadora cache TTL: " + CACHE_TTL.toMinutes() + " minutes";
    }
}
