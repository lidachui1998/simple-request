package com.lidachui.simpleRequest.annotation;

import java.lang.annotation.*;

/**
 * ResponseHeader
 *
 * @author: lihuijie
 * @date: 2024/11/23 11:27
 * @version: 1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseHeader {

    String name() default "";
}
