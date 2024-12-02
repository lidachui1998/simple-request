package com.lidachui.simpleRequest.annotation;

import java.lang.annotation.*;

/**
 * HessianClient
 *
 * @author: lihuijie
 * @date: 2024/12/2 23:01
 * @version: 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HessianClient {

    /**
     * 名称
     *
     * @return 一串
     */
    String name() default "";

    /**
     * 基本url
     *
     * @return 一串
     */
    String baseUrl() default "";

    /**
     * 属性密钥
     *
     * @return 一串
     */
    String propertyKey() default "";
}
