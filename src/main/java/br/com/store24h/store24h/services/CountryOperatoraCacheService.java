/*
 * Country and Operator validation service with Redis caching
 * Uses chip_model table for strict validation
 */
package br.com.store24h.store24h.services;

import br.com.store24h.store24h.model.ChipModel;
import br.com.store24h.store24h.repository.ChipRepository;
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
    private ChipRepository chipRepository;
    
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    /**
     * Check if country+operator combination is valid using chip_model table
     * STRICT VALIDATION: Only allows combinations that actually exist in chip_model
     */
    @Cacheable(value = "countryOperadoraValidation", key = "#country + ':' + #operator")
    public boolean isValidCombination(String country, String operator) {
        try {
            if (operator.equalsIgnoreCase("any")) {
                // For "any" operator, check if ANY operator exists for this country in chip_model
                List<ChipModel> anyOperator = chipRepository.findByCountryAndAlugadoAndAtivo(country, false, true);
                boolean isValid = !anyOperator.isEmpty();
                logger.info("🔍 ANY operator validation for country {}: {} (found {} numbers)", country, isValid, anyOperator.size());
                return isValid;
            }
            
            // Check specific country+operator combination in chip_model
            List<ChipModel> specificOperator = chipRepository.findByCountryAndAlugadoAndAtivoAndOperadora(
                country, false, true, operator.toLowerCase());
            boolean isValid = !specificOperator.isEmpty();
            
            logger.info("🔍 Specific operator validation for {}:{} = {} (found {} numbers)", 
                country, operator, isValid, specificOperator.size());
            
            if (!isValid) {
                logger.warn("❌ INVALID COMBINATION: country={}, operator={} - NO NUMBERS FOUND", country, operator);
            }
            
            return isValid;
                               
        } catch (Exception e) {
            logger.error("❌ Error validating {}:{}", country, operator, e);
            return false; // STRICT: Fail closed - deny if can't verify
        }
    }
    
    /**
     * Check if country exists using chip_model table
     * STRICT VALIDATION: Only allows countries that have actual numbers
     */
    public boolean isValidCountry(String country) {
        try {
            List<ChipModel> countryNumbers = chipRepository.findByCountryAndAlugadoAndAtivo(country, false, true);
            boolean isValid = !countryNumbers.isEmpty();
            
            logger.info("🔍 Country validation for {}: {} (found {} numbers)", country, isValid, countryNumbers.size());
            
            if (!isValid) {
                logger.warn("❌ INVALID COUNTRY: {} - NO NUMBERS FOUND", country);
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("❌ Error validating country: {}", country, e);
            return false; // STRICT: Fail closed
        }
    }
    
    /**
     * Get all valid operators for a specific country using chip_model
     */
    public List<String> getOperatorsForCountry(String country) {
        try {
            List<ChipModel> countryNumbers = chipRepository.findByCountryAndAlugadoAndAtivo(country, false, true);
            List<String> operators = countryNumbers.stream()
                .map(ChipModel::getOperadora)
                .distinct()
                .collect(Collectors.toList());
                
            logger.info("🔍 Found {} operators for country {}: {}", operators.size(), country, operators);
            return operators;
        } catch (Exception e) {
            logger.error("❌ Error getting operators for country: {}", country, e);
            return List.of(); // Return empty list on error
        }
    }
    
    /**
     * Get all valid countries using chip_model
     */
    public List<String> getAllValidCountries() {
        try {
            List<ChipModel> allNumbers = chipRepository.findByAlugadoAndAtivoRandomOrderWithLimit(false, true);
            List<String> countries = allNumbers.stream()
                .map(ChipModel::getCountry)
                .distinct()
                .collect(Collectors.toList());
                
            logger.info("🔍 Found {} valid countries: {}", countries.size(), countries);
            return countries;
        } catch (Exception e) {
            logger.error("❌ Error getting valid countries", e);
            return List.of(); // Return empty list on error
        }
    }
    
    /**
     * Warm up the cache using chip_model data
     */
    public void warmUpCache() {
        try {
            logger.info("🔥 Warming up country+operator validation cache using chip_model...");
            
            List<ChipModel> allNumbers = chipRepository.findByAlugadoAndAtivoRandomOrderWithLimit(false, true);
            
            // Pre-validate all country+operator combinations
            for (ChipModel chip : allNumbers) {
                isValidCombination(chip.getCountry(), chip.getOperadora());
                isValidCountry(chip.getCountry());
            }
            
            logger.info("✅ Cache warmed up with {} chip_model records", allNumbers.size());
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
