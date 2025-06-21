package com.lidachui.simpleRequest.annotation;

import com.lidachui.simpleRequest.cache.CacheStrategy;
import com.lidachui.simpleRequest.cache.LocalCacheStrategy;
import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

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
    long expire() default 60; // 缓存过期时间，默认60

    TimeUnit timeUnit() default TimeUnit.SECONDS; // 时间单位，默认秒

    Class<? extends CacheStrategy> strategy() default LocalCacheStrategy.class; // 缓存策略类
}