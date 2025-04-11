package com.lidachui.simpleRequest.cache;

import com.lidachui.simpleRequest.constants.CacheEventType;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * CacheEvent
 *
 * @author: lihuijie
 * @date: 2025/2/18 10:28
 * @version: 1.0
 */
@Data
@AllArgsConstructor
public class CacheEvent {
    private String key;
    private Object value;
    private CacheEventType eventType;
    private long timestamp;

    public CacheEvent(String key, Object value, CacheEventType eventType) {
        this.key = key;
        this.value = value;
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }
}
