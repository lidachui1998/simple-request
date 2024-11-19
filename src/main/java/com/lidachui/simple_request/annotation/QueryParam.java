package com.lidachui.simple_request.annotation;

import java.lang.annotation.*;

/**
 * Param
 *
 * @author: lihuijie
 * @date: 2024/11/18 21:56
 * @version: 1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryParam {
    String value(); // 参数名
}