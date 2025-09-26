package br.com.store24h.store24h;

import org.springframework.boot.SpringApplication;

/**
 * Cron Tasks Application - DISABLED FOR PRODUCTION
 * All scheduled tasks and background processing have been disabled
 * 
 * @author Archer (brainuxdev@gmail.com)
 */
// @EnableCaching - DISABLED FOR PRODUCTION
// @EnableScheduling - DISABLED: No cron tasks should run in production
// @EnableTransactionManagement - DISABLED FOR PRODUCTION
// @SpringBootApplication - DISABLED: This class is not used in production
public class Store24hCronApplication {

    public static void main(String[] args) {
        System.out.println("⚠️  CRON APPLICATION DISABLED - No scheduled tasks will run");
        System.out.println("🚀 Starting API-only mode for production deployment");
        // Redirect to API application instead of running cron
        SpringApplication.run(Store24hApiApplication.class, args);
    }
}
