package com.example.notifications.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@Profile("!test")
public class CacheConfig {

    @Value("${spring.data.redis.time-to-live}")
    private long timeToLiveMs;

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer jsonSerializer = GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(mapper)
                .defaultTyping(true)
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(timeToLiveMs))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer))
                .disableCachingNullValues();
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory cf,
            RedisCacheConfiguration cacheConfig) {
        return RedisCacheManager.builder(cf)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
