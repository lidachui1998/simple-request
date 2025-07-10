package com.lidachui.simpleRequest.cache;



import com.lidachui.simpleRequest.constants.CacheEventType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import org.springframework.util.CollectionUtils;

import static com.lidachui.simpleRequest.util.HashBasedCacheKeyGenerator.FRAMEWORK_IDENTIFIER;

/**
 * RedisCacheStrategy
 *
 * @author: lihuijie
 * @date: 2024/12/5 14:13
 * @version: 1.0
 */
public class RedisCacheStrategy implements CacheStrategy {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheStrategy.class);

    private final List<CacheEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(CacheEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(CacheEventListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(CacheEvent event) {
        for (CacheEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                logger.error("Error notifying listener: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Object get(String key) {
        if (redisTemplate == null) {
            logger.warn("Redis is not configured or disabled.");
            return null;
        }
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            notifyListeners(new CacheEvent(key, value, CacheEventType.GET));
        }
        return value;
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        if (redisTemplate == null) {
            logger.warn("Redis is not configured or disabled.");
            return;
        }
        redisTemplate.opsForValue().set(key, value, expire, timeUnit);
        notifyListeners(new CacheEvent(key, value, CacheEventType.PUT));
    }

    @Override
    public void remove(String key) {
        if (redisTemplate == null) {
            logger.warn("Redis is not configured or disabled.");
            return;
        }
        Object value = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (value != null) {
            notifyListeners(new CacheEvent(key, value, CacheEventType.REMOVE));
        }
    }

    @Override
    public void removeAll() {
        if (redisTemplate != null) {
            Set<String> keys = redisTemplate.keys(FRAMEWORK_IDENTIFIER + "*");
            if (!CollectionUtils.isEmpty(keys)) {
                redisTemplate.delete(keys);
            }
        }
    }
}
