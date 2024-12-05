package com.lidachui.simpleRequest.annotation;

import com.lidachui.simpleRequest.cache.CacheStrategy;
import com.lidachui.simpleRequest.cache.LocalCacheStrategy;
import java.lang.annotation.*;

/**
 * Cacheable
 *
 * @author: lihuijie
 * @date: 2024/12/5 14:08
 * @version: 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {
    long ttl() default 60000; // 缓存时间，默认60秒

    Class<? extends CacheStrategy> strategy() default LocalCacheStrategy.class; // 缓存策略类
}
