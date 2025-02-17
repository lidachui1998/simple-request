package com.lidachui.simpleRequest.annotation;

import com.lidachui.simpleRequest.autoconfigure.RestClientAutoConfiguration;
import com.lidachui.simpleRequest.core.RestClientRegistrar;
import com.lidachui.simpleRequest.core.RestClientFactoryBean;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * EnableRestClients
 *
 * @author: lihuijie
 * @date: 2024/11/19 9:58
 * @version: 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({RestClientRegistrar.class, RestClientAutoConfiguration.class})
public @interface EnableRestClients {
    String[] value() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

    Class<? extends Annotation> annotationClass() default Annotation.class;

    Class<?> markerInterface() default Class.class;

    Class<? extends RestClientFactoryBean> factoryBean() default RestClientFactoryBean.class;
}
