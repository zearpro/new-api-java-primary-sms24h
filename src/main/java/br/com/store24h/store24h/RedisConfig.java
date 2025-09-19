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
 *  org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
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
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
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
        if (System.getenv("USE_REDIS_LOCAL") != null) {
            return new LettuceConnectionFactory("redis", 6379);
        }
        if (Utils.getVersion().equals((Object)VersionEnum.VERSION_2) || System.getenv("USE_REDIS_REMOTE") != null) {
            List<String> redisArray = Arrays.asList("redis-api-versao-2-0001-001.redis-api-versao-2.uih1si.memorydb.us-east-2.amazonaws.com:6379", "redis-api-versao-2-0001-002.redis-api-versao-2.uih1si.memorydb.us-east-2.amazonaws.com:6379");
            RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(redisArray);
            System.out.println("Redis login " + redisArray.toString());
            clusterConfig.setMaxRedirects(3);
            clusterConfig.setPassword("vv10Fn0c4P8AlT8S");
            clusterConfig.setUsername("admin-user");
            LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder().useSsl().disablePeerVerification().build();
            return new LettuceConnectionFactory(clusterConfig, (LettuceClientConfiguration)clientConfig);
        }
        return new LettuceConnectionFactory("redis", 6379);
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
