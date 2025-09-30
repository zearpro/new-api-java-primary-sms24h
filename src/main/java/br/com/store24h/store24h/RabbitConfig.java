/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.amqp.core.AmqpAdmin
 *  org.springframework.amqp.rabbit.connection.CachingConnectionFactory
 *  org.springframework.amqp.rabbit.connection.ConnectionFactory
 *  org.springframework.amqp.rabbit.core.RabbitAdmin
 *  org.springframework.amqp.rabbit.core.RabbitTemplate
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package br.com.store24h.store24h;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public ConnectionFactory connectionFactory() {
        String host = System.getenv("RABBITMQ_HOST");
        if (host == null) {
            host = "rabbitmq";
        }
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host);
        connectionFactory.setUsername("guesta");
        connectionFactory.setPassword("guesta");
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * Phase 1 Velocity Layer - Number assignment queue
     * Handles async number assignment processing
     */
    @Bean
    public org.springframework.amqp.core.Queue numberAssignedQueue() {
        return org.springframework.amqp.core.QueueBuilder
            .durable("number.assigned")
            .withArgument("x-message-ttl", 300000) // 5 minutes TTL for safety
            .build();
    }

    /**
     * Compatibility queue: consumer listens to 'number-assignment-queue'
     */
    @Bean
    public org.springframework.amqp.core.Queue numberAssignmentQueue() {
        return org.springframework.amqp.core.QueueBuilder
            .durable("number-assignment-queue")
            .build();
    }

    /**
     * Cache invalidation queue used by consumer
     */
    @Bean
    public org.springframework.amqp.core.Queue cacheInvalidationQueue() {
        return org.springframework.amqp.core.QueueBuilder
            .durable("cache-invalidation-queue")
            .build();
    }

    /**
     * Health check queue for monitoring
     */
    @Bean
    public org.springframework.amqp.core.Queue healthCheckQueue() {
        return org.springframework.amqp.core.QueueBuilder
            .durable("health.check")
            .withArgument("x-message-ttl", 30000) // 30 seconds TTL
            .build();
    }
}
