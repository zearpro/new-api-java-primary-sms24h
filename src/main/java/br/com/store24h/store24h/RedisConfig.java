/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
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

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
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
		String redisHost = System.getenv("REDIS_HOST");
		String redisPort = System.getenv("REDIS_PORT");
		String redisPassword = System.getenv("REDIS_PASSWORD");
		String redisUsername = System.getenv("REDIS_USERNAME");
		String redisSsl = System.getenv("REDIS_SSL");

		if (redisHost == null) redisHost = "dragonfly";
		if (redisPort == null) redisPort = "6379";
		if (redisPassword == null) redisPassword = "";

		int port = Integer.parseInt(redisPort);
		boolean useSsl = redisSsl != null ? Boolean.parseBoolean(redisSsl) : false;

		RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(redisHost, port);
		if (redisUsername != null && !redisUsername.isEmpty()) {
			standalone.setUsername(redisUsername);
		}
		if (redisPassword != null && !redisPassword.isEmpty()) {
			standalone.setPassword(RedisPassword.of(redisPassword));
		}

		LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder()
				.commandTimeout(Duration.ofSeconds(30));
		if (useSsl) {
			builder.useSsl();
		}

		LettuceClientConfiguration clientConfig = builder.build();
		LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfig);
		factory.setValidateConnection(true);
		return factory;
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		RedisSerializer<String> keySerializer = new StringRedisSerializer();
		RedisSerializer<Object> valueSerializer = new GenericToStringSerializer<>(Object.class);
		template.setKeySerializer(keySerializer);
		template.setValueSerializer(valueSerializer);
		return template;
	}
}
