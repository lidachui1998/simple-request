package com.lidachui.simpleRequest.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HeaderParam
 *
 * @author: lihuijie
 * @date: 2024/11/19 11:20
 * @version: 1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HeaderParam {
    String value() default ""; // 参数名
}
