package com.lidachui.simpleRequest.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

/**
 * RedisCacheStrategy
 *
 * @author: lihuijie
 * @date: 2024/12/5 14:13
 * @version: 1.0
 */
public class RedisCacheStrategy implements CacheStrategy {

    @Resource private RedisTemplate redisTemplate;

    @Resource private StringRedisTemplate stringRedisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheStrategy.class);

    @Override
    public Object get(String key) {
        if (redisTemplate == null) {
            logger.warn("Redis is not configured or disabled. Skipping cache retrieval.");
            return null;
        }
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }

    @Override
    public void put(String key, Object value, long ttl) {
        if (redisTemplate == null) {
            logger.warn("Redis is not configured or disabled. Skipping cache storage.");
            return;
        }
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value, ttl, TimeUnit.MILLISECONDS);
    }
}
