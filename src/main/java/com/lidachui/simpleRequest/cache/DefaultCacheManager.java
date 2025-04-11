package com.lidachui.simpleRequest.cache;

import java.util.List;

/**
 * DefaultCacheManager
 *
 * @author: lihuijie
 * @date: 2025/2/18 11:06
 * @version: 1.0
 */
public class DefaultCacheManager implements CacheManager {

    private final List<CacheStrategy> strategies;

    public DefaultCacheManager(List<CacheStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 注册缓存监听器
     *
     * @param listener 听众
     * @param strategy
     */
    @Override
    public void registerListener(
            CacheEventListener listener, Class<? extends CacheStrategy> strategy) {
        strategies.stream()
                .filter(s -> s.getClass().equals(strategy))
                .findFirst()
                .ifPresent(s -> s.addListener(listener));
    }

    /**
     * 手动触发缓存清理
     *
     * @param key 钥匙
     * @param strategy
     */
    @Override
    public void invalidateCache(String key, Class<? extends CacheStrategy> strategy) {
        strategies.stream()
                .filter(s -> s.getClass().equals(strategy))
                .findFirst()
                .ifPresent(s -> s.remove(key));
    }

    /** 全部无效 */
    @Override
    public void invalidateAll() {
        strategies.forEach(CacheStrategy::removeAll);
    }
}
