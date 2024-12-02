package com.lidachui.simpleRequest.util;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.util.*;
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
     * 获取指定包中带有特定注解的所有类。
     *
     * @param packageName 包名
     * @param annotations 要查找的注解类
     * @return 带有指定注解的类列表
     */
    public static List<Class<?>> getClassesWithAnnotations(String packageName, Class<? extends Annotation>... annotations) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(packageName)
                .addScanners(Scanners.TypesAnnotated)); // Scanners.TypesAnnotated 用于扫描类上的注解

        Set<Class<?>> annotatedClasses = new HashSet<>();

        // 遍历所有注解，获取标注了其中任何一个注解的类
        for (Class<? extends Annotation> annotation : annotations) {
            Set<Class<?>> classesWithAnnotation = reflections.getTypesAnnotatedWith(annotation);
            annotatedClasses.addAll(classesWithAnnotation);
        }

        return annotatedClasses.stream().collect(Collectors.toList());
    }

}
