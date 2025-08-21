package com.lidachui.simpleRequest.annotation;

import java.lang.annotation.*;

/**
 * Async 异步请求
 *
 * @author lihuijie
 * @date 2024/12/04
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Async {
}
