package com.lidachui.simpleRequest.annotation;

import com.lidachui.simpleRequest.auth.AuthProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Auth
 *
 * @author: lihuijie
 * @date: 2024/11/22 23:47
 * @version: 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {
    Class<? extends AuthProvider> provider();
}
