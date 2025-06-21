package com.lidachui.simpleRequest.cache;

import java.util.concurrent.TimeUnit;

/**
 * CacheStrategy
 *
 * @author: lihuijie
 * @date: 2024/12/5 14:12
 * @version: 1.0
 */
public interface CacheStrategy {

    Object get(String key);

    void put(String key, Object value, long expire, TimeUnit timeUnit);

    void addListener(CacheEventListener listener);

    void removeListener(CacheEventListener listener);

    void remove(String key);  // 新增删除方法

    void removeAll();
}
