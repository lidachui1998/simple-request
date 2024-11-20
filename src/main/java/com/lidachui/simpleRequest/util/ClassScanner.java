package com.lidachui.simpleRequest.util;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * 类用于扫描指定包中的类，支持注解扫描或基于自定义过滤条件扫描类。
 *
 * @author: lihuijie
 * @date: 2024/11/19 13:37
 * @version: 1.0
 */
public class ClassScanner {

    /**
     * 获取指定包中带有特定注解的所有类。
     *
     * @param packageName 包名
     * @param annotation 要查找的注解类
     * @return 带有指定注解的类列表
     */
    public static List<Class<?>> getClassesWithAnnotation(String packageName, Class<? extends Annotation> annotation) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(packageName)
                .addScanners(Scanners.TypesAnnotated)); // Scanners.TypesAnnotated 用于扫描类上的注解
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(annotation);
        return annotatedClasses.stream().collect(Collectors.toList());
    }

    /**
     * 获取指定包中符合自定义过滤条件的所有类。
     *
     * @param packageName 包名
     * @param filter 过滤器接口，用于定义过滤条件
     * @return 符合条件的类列表
     */
    public static List<Class<?>> getClassesWithFilter(String packageName, ClassFilter filter) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(packageName)
                .addScanners(Scanners.SubTypes)); // Scanners.SubTypes 用于扫描子类或接口实现类
        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class); // 获取所有类
        return allClasses.stream().filter(filter::matches).collect(Collectors.toList());
    }

    /** ClassFilter 接口用于定义类过滤条件。 */
    public interface ClassFilter {
        /**
         * 检查类是否符合条件。
         *
         * @param clazz 要检查的类
         * @return 如果符合条件返回 true，否则返回 false
         */
        boolean matches(Class<?> clazz);
    }
}