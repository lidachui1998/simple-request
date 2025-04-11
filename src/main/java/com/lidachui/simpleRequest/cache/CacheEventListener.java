package com.lidachui.simpleRequest.cache;

/**
 * CacheEventListener
 *
 * @author: lihuijie
 * @date: 2025/2/18 10:26
 * @version: 1.0
 */
public interface CacheEventListener {

    /**
     * 缓存事件
     *
     * @param event 事件
     */
    void onEvent(CacheEvent event);
}
