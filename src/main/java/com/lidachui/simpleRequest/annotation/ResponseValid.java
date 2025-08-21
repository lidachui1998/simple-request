package com.lidachui.simpleRequest.annotation;

import com.lidachui.simpleRequest.validator.DefaultResponseValidator;
import com.lidachui.simpleRequest.validator.ResponseValidator;

import java.lang.annotation.*;

/**
 * ResponseValid
 *
 * @author: lihuijie
 * @date: 2025/8/22 0:05
 * @version: 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseValid {

    /**
     * 响应验证器
     *
     * @return {@code Class<? extends ResponseValidator> }
     */
    Class<? extends ResponseValidator> responseValidator() default DefaultResponseValidator.class;
}
