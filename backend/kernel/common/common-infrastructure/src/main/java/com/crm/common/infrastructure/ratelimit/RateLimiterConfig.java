package com.crm.common.infrastructure.ratelimit;

import com.crm.common.application.ratelimit.RateLimiter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Liga o {@link RateLimiter} sobre o Redis.
 */
@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterConfig {

    @Bean
    public RateLimiter rateLimiter(StringRedisTemplate redisTemplate, RateLimiterProperties properties) {
        return new RedisRateLimiterImpl(redisTemplate, properties);
    }
}
