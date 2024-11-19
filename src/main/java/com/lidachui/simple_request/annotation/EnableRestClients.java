package com.lidachui.simple_request.annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.lidachui.simple_request.core.RestClientRegistrar;
import org.springframework.context.annotation.Import;

/**
 * EnableRestClients
 * @author: lihuijie
 * @date: 2024/11/19 9:58
 * @version: 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RestClientRegistrar.class)
public @interface EnableRestClients {

  String[] basePackages() default {};

}
