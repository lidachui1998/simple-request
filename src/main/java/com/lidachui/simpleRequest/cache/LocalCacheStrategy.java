package com.lidachui.simpleRequest.cache;

import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LocalCacheStrategy
 *
 * @author: lihuijie
 * @date: 2024/12/5 14:13
 * @version: 1.0
 */
public class LocalCacheStrategy implements CacheStrategy {
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicBoolean isTaskRunning = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduledFuture;

    @Override
    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired(System.currentTimeMillis())) {
            return entry.getValue();
        }
        return null;
    }

    @Override
    public void put(String key, Object value, long ttl) {
        cache.put(key, new CacheEntry(value, ttl));
        startCleanupTask();
    }

    private void startCleanupTask() {
        if (isTaskRunning.compareAndSet(false, true)) {
            scheduledFuture =
                    scheduler.scheduleWithFixedDelay(this::cleanUp, 1, 1, TimeUnit.MINUTES);
        }
    }

    private void cleanUp() {
        long currentTime = System.currentTimeMillis();
        cache.keySet()
                .removeIf(
                        key -> {
                            CacheEntry entry = cache.get(key);
                            return entry != null && entry.isExpired(currentTime);
                        });

        if (cache.isEmpty()) {
            stopCleanupTask();
        }
    }

    private void stopCleanupTask() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            isTaskRunning.set(false);
        }
    }

    private static class CacheEntry {
        private final Object value;
        private final long expiryTime;

        CacheEntry(Object value, long ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl;
        }

        boolean isExpired(long currentTime) {
            return currentTime > expiryTime;
        }

        Object getValue() {
            return value;
        }
    }
}
