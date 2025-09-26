/**
 *
 * Copyright (c) 2022, 2023, AP Codes and/or its affiliates. All rights reserved.
 * AP Codes PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package br.com.store24h.store24h;

import org.springframework.boot.SpringApplication;

/**
 *
 * @author Archer (brainuxdev@gmail.com)
 * 
 * This is the legacy main class. For production use:
 * - Store24hApiApplication.java - for REST API services
 * - Store24hCronApplication.java - for background processing
 */
// Legacy launcher class - Spring Boot annotations removed to prevent conflicts
// @EnableCaching - MOVED to Store24hApiApplication
// @EnableTransactionManagement - MOVED to Store24hApiApplication
// @SpringBootApplication - REMOVED to prevent duplicate configuration
public class Store24hApplication {

  public static void main(String[] args) {
    // Check if specific profile is requested via environment variable or system property
    String profile = System.getenv("SPRING_PROFILES_ACTIVE");
    if (profile == null) {
      profile = System.getProperty("spring.profiles.active");
    }
    
    if ("cron".equals(profile)) {
      System.out.println("⚠️  CRON DISABLED: All scheduled tasks disabled for production");
      System.out.println("🚀 Starting API Service instead...");
      SpringApplication.run(Store24hApiApplication.class, args);
    } else {
      System.out.println("🚀 Starting API Service...");
      SpringApplication.run(Store24hApiApplication.class, args);
    }
  }
}
