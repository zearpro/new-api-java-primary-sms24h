/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.cache.annotation.EnableCaching
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.data.redis.cache.RedisCacheConfiguration
 *  org.springframework.data.redis.cache.RedisCacheManager
 *  org.springframework.data.redis.connection.RedisConnectionFactory
 *  org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
 *  org.springframework.data.redis.serializer.RedisSerializationContext$SerializationPair
 *  org.springframework.data.redis.serializer.RedisSerializer
 */
package br.com.store24h.store24h;

import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(48L)).serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer((RedisSerializer)new GenericJackson2JsonRedisSerializer())).disableCachingNullValues();
        RedisCacheConfiguration servicosByActivityConfig = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(4L)).disableCachingNullValues();
        RedisCacheConfiguration configuracao1min = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1L)).disableCachingNullValues();
        RedisCacheConfiguration configuracao1Hora = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1L)).disableCachingNullValues();
        RedisCacheConfiguration configuracao5min = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5L)).disableCachingNullValues();
        RedisCacheConfiguration configuracao24Horas = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24L)).disableCachingNullValues();
        RedisCacheConfiguration configuracao10min = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10L)).disableCachingNullValues();
        RedisCacheConfiguration configuracao20min = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(20L)).disableCachingNullValues();
        RedisCacheConfiguration userBalanceConfig = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(30L)).disableCachingNullValues();
        return RedisCacheManager.builder((RedisConnectionFactory)connectionFactory).cacheDefaults(defaultCacheConfig).withCacheConfiguration("servicosByActivity", servicosByActivityConfig).withCacheConfiguration("findFirstByEmail", configuracao1min).withCacheConfiguration("findFirstByApiKey", configuracao1min).withCacheConfiguration("findUserByApiKey", configuracao1min).withCacheConfiguration("findUserByApiKeyCache", configuracao1min).withCacheConfiguration("findUserByApiKeyCache2", configuracao1min).withCacheConfiguration("getApiCallbackFromCache", configuracao1Hora).withCacheConfiguration("getServiceCache", configuracao1min).withCacheConfiguration("findUserApiType", configuracao1min).withCacheConfiguration("findFirstById", configuracao1min).withCacheConfiguration("findFirstByAlias", configuracao1min).withCacheConfiguration("getNumerosDisponiveisSemFiltrarNumerosPreviosCache", configuracao20min).withCacheConfiguration("getLatestNumerosDisponiveisSemFiltrarNumerosPreviosCache", configuracao10min).withCacheConfiguration("comprasFindLatestIdByIdActivationCache", configuracao1Hora).withCacheConfiguration("userBalance", userBalanceConfig).build();
    }
}
