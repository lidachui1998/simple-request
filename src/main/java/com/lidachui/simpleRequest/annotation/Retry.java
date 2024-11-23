package com.lidachui.simpleRequest.annotation;

import com.lidachui.simpleRequest.constants.BackoffStrategy;
import com.lidachui.simpleRequest.validator.DefaultResponseValidator;
import com.lidachui.simpleRequest.validator.ResponseValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Retry
 *
 * @author: lihuijie
 * @date: 2024/11/23 0:08
 * @version: 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Retry {
    /**
     * 最大重试次数
     *
     * @return int
     */
    int maxRetries() default 3;

    /**
     * 延迟
     *
     * @return long
     */
    long delay() default 1000; // 默认间隔 1 秒

    /**
     * 重试对于哪些异常进行重试
     *
     * @return 类<？ 扩展throwable>[]
     */
    Class<? extends Throwable>[] retryFor() default Exception.class;

    /**
     * 固定间隔或指数退避
     *
     * @return BackoffStrategy
     */
    BackoffStrategy backoff() default BackoffStrategy.FIXED;
}
