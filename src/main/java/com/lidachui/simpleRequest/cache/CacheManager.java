package com.lidachui.simpleRequest.cache;



/**
 * CacheManager
 *
 * @author: lihuijie
 * @date: 2025/2/18 10:26
 * @version: 1.0
 */
public interface CacheManager {
    /**
     * 注册缓存监听器
     *
     * @param listener 听众
     */
    void registerListener(CacheEventListener listener, Class<? extends CacheStrategy> strategy);

    /**
     * 手动触发缓存清理
     *
     * @param key 钥匙
     */
    void invalidateCache(String key, Class<? extends CacheStrategy> strategy);

    /** 全部无效 */
    void invalidateAll();
}
