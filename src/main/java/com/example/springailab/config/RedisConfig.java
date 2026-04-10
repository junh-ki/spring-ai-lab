package com.example.springailab.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, List<Double>> embeddingRedisTemplate(final RedisConnectionFactory redisConnectionFactory) {
        final RedisTemplate<String, List<Double>> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // Use String serialization for keys (readable hashes)
        redisTemplate.setValueSerializer(RedisSerializer.json()); // Use JSON serialization for vector data
        return redisTemplate;
    }
}
