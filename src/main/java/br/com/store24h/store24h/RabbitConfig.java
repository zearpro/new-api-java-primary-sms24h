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
}
