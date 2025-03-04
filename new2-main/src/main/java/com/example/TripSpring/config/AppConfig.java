package com.example.TripSpring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class AppConfig {

    // @Bean
    // public RedisConnectionFactory redisConnectionFactory() {
    //     return new LettuceConnectionFactory();
    // }
    
    // @Bean
    // public CacheManager cacheManager(RedisConnectionFactory factory) {
    //     return RedisCacheManager.builder(factory).build();
    // }
}