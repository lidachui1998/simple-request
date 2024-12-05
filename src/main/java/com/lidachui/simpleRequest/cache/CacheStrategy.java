package com.lidachui.simpleRequest.cache;

/**
 * CacheStrategy
 *
 * @author: lihuijie
 * @date: 2024/12/5 14:12
 * @version: 1.0
 */
public interface CacheStrategy {
    /**
     * 获取缓存数据
     *
     * @param key 钥匙
     * @return {@code Object }
     */
    Object get(String key);

    /**
     * 缓存数据
     *
     * @param key 钥匙
     * @param value 价值
     * @param ttl ttl
     */
    void put(String key, Object value, long ttl);
}
