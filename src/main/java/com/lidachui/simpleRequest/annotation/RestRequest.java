package com.lidachui.simpleRequest.annotation;

import org.springframework.http.HttpMethod;

import java.lang.annotation.*;

/**
 * RestRequest
 *
 * @author: lihuijie
 * @date: 2024/11/18 21:50
 * @version: 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestRequest {
    /**
     * 路径
     *
     * @return {@code String }
     */
    String path(); // 请求路径

    /**
     * 方法
     *
     * @return {@code HttpMethod }
     */
    HttpMethod method() default HttpMethod.GET;

    /**
     * Headers，格式如 "key:value"
     *
     * @return {@code String[] }
     */
    String[] headers() default {};

    /**
     * 查询参数参数 Query 参数，格式如 "key=value"
     *
     * @return {@code String[] }
     */
    String[] queryParams() default {};
}
