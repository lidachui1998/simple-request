package com.lidachui.simpleRequest.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.util.*;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * SpringUtil
 *
 * @author: lihuijie
 * @date: 2024/11/24 0:59
 * @version: 1.0
 */
@Slf4j
public class SpringUtil implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtil.context = applicationContext;
    }

    @Nullable
    public static <T> T getBean(@NonNull String beanName) {
        try {
            return (T) context.getBean(beanName);
        } catch (BeansException e) {
            log.error("Failed to get bean with name [{}]: {}", beanName, e.getMessage());
            return null;
        }
    }

    /** 获取指定类型的 Bean 实例 */
    @Nullable
    public static <T> T getBean(@NonNull Class<T> clazz) {
        try {
            return context.getBean(clazz);
        } catch (BeansException e) {
            log.error(
                    "Failed to get bean with type [{}]: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /** 获取指定类型和名称的 Bean 实例 */
    @Nullable
    public static <T> T getBean(@NonNull Class<T> clazz, @NonNull String beanName) {
        try {
            return context.getBean(beanName, clazz);
        } catch (BeansException e) {
            log.error(
                    "Failed to get bean with name [{}] and type [{}]: {}",
                    beanName,
                    clazz.getSimpleName(),
                    e.getMessage());
            return null;
        }
    }

    /** 获取指定类型和注解的 Bean 实例 */
    @Nullable
    public static <T> T getBean(
            @NonNull Class<T> clazz, @NonNull Class<? extends Annotation> qualifier) {
        try {
            return context.getBean(clazz, qualifier);
        } catch (BeansException e) {
            log.error(
                    "Failed to get bean with qualifier [{}] and type [{}]: {}",
                    qualifier.getSimpleName(),
                    clazz.getSimpleName(),
                    e.getMessage());
            return null;
        }
    }

    /**
     * 返回该类型所有的bean
     *
     * @param clazz
     * @return
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return context.getBeansOfType(clazz);
    }

    /**
     * 注册指定类到 Spring 上下文中作为一个 Bean
     *
     * @param beanClass 要注册的类
     */
    public static void registerBean(Class<?> beanClass) {
        if (context instanceof GenericWebApplicationContext) {
            GenericWebApplicationContext applicationContext =
                    (GenericWebApplicationContext) context;
            applicationContext.registerBean(beanClass); // 注册 Bean
        } else {
            throw new IllegalArgumentException(
                    "ApplicationContext 不是 GenericWebApplicationContext 类型，无法注册 Bean");
        }
    }
}
