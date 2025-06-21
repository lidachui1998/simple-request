package com.lidachui.simpleRequest.cache;

import com.lidachui.simpleRequest.constants.CacheEventType;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LocalCacheStrategy
 *
 * @author: lihuijie
 * @date: 2024/12/5 14:13
 * @version: 1.0
 */
@Slf4j
public class LocalCacheStrategy implements CacheStrategy {
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final List<CacheEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicBoolean isTaskRunning = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduledFuture;

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
                log.error("Error notifying listener: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            if (entry.isExpired(System.currentTimeMillis())) {
                notifyListeners(new CacheEvent(key, entry.getValue(), CacheEventType.EXPIRE));
                cache.remove(key);
                return null;
            }
            notifyListeners(new CacheEvent(key, entry.getValue(), CacheEventType.GET));
            return entry.getValue();
        }
        return null;
    }

    @Override
    public void put(String key, Object value, long expire, TimeUnit timeUnit) {
        // 将过期时间转换为毫秒
        long expireMillis = timeUnit.toMillis(expire);
        cache.put(key, new CacheEntry(value, expireMillis));
        notifyListeners(new CacheEvent(key, value, CacheEventType.PUT));
        startCleanupTask();
    }

    @Override
    public void remove(String key) {
        CacheEntry entry = cache.remove(key);
        if (entry != null) {
            notifyListeners(new CacheEvent(key, entry.getValue(), CacheEventType.REMOVE));
        }
    }

    @Override
    public void removeAll() {
        cache.clear();
        // 清空所有缓存后，停止清理任务
        stopCleanupTask();
    }

    private void startCleanupTask() {
        if (isTaskRunning.compareAndSet(false, true)) {
            scheduledFuture =
                    scheduler.scheduleWithFixedDelay(this::cleanUp, 1, 1, TimeUnit.MINUTES);
        }
    }

    private void cleanUp() {
        long currentTime = System.currentTimeMillis();
        cache.entrySet()
                .removeIf(
                        entry -> {
                            if (entry.getValue().isExpired(currentTime)) {
                                notifyListeners(
                                        new CacheEvent(
                                                entry.getKey(),
                                                entry.getValue().getValue(),
                                                CacheEventType.EXPIRE));
                                return true;
                            }
                            return false;
                        });

        if (cache.isEmpty()) {
            stopCleanupTask();
        }
    }

    private void stopCleanupTask() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
            isTaskRunning.set(false);
        }
    }

    /**
     * 关闭清理任务线程池
     */
    public void shutdown() {
        stopCleanupTask();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class CacheEntry {
        private final Object value;
        private final long expiryTime;

        CacheEntry(Object value, long expireMillis) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + expireMillis;
        }

        boolean isExpired(long currentTime) {
            return currentTime > expiryTime;
        }

        Object getValue() {
            return value;
        }
    }
}