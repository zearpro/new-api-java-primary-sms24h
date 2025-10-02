/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.data.redis.connection.RedisClusterConfiguration
 *  org.springframework.data.redis.connection.RedisConnectionFactory
 *  org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
 *  org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
 *  org.springframework.data.redis.core.RedisTemplate
 *  org.springframework.data.redis.serializer.GenericToStringSerializer
 *  org.springframework.data.redis.serializer.RedisSerializer
 *  org.springframework.data.redis.serializer.StringRedisSerializer
 *  org.springframework.session.data.redis.config.ConfigureRedisAction
 */
package br.com.store24h.store24h;

import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.VersionEnum;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.ConfigureRedisAction;

@Configuration
public class RedisConfig {
    @Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // Get Redis configuration from environment variables
        String redisHost = System.getenv("REDIS_HOST");
        String redisPort = System.getenv("REDIS_PORT");
        String redisPassword = System.getenv("REDIS_PASSWORD");
        
        // Default to DragonflyDB container configuration
        if (redisHost == null) redisHost = "dragonfly";
        if (redisPort == null) redisPort = "6379";
        if (redisPassword == null) redisPassword = "";
        
        int port = Integer.parseInt(redisPort);
        
        // Minimal Redis connection logging - only on errors
        
        // Create simple connection factory for Redis container
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisHost, port);
        factory.setPassword(redisPassword);
        factory.setValidateConnection(true);
        
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate template = new RedisTemplate();
        template.setConnectionFactory((RedisConnectionFactory)connectionFactory);
        template.setKeySerializer((RedisSerializer)new StringRedisSerializer());
        template.setValueSerializer((RedisSerializer)new GenericToStringSerializer(Object.class));
        return template;
    }
}
