package br.com.store24h.store24h;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main API Application - High Performance Version
 * Handles all REST API requests without cron tasks
 * 
 * @author Archer (brainuxdev@gmail.com)
 */
@EnableCaching
@EnableTransactionManagement
@SpringBootApplication
public class Store24hApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(Store24hApiApplication.class, args);
    }
}
