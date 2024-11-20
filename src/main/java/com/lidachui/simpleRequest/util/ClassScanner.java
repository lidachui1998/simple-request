package com.lidachui.simpleRequest.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
     * @throws Exception 可能抛出的异常
     */
    public static List<Class<?>> getClassesWithAnnotation(
            String packageName, Class<? extends Annotation> annotation) throws Exception {
        return getClassesWithFilter(packageName, clazz -> clazz.isAnnotationPresent(annotation));
    }

    /**
     * 获取指定包中符合自定义过滤条件的所有类。
     *
     * @param packageName 包名
     * @param filter 过滤器接口，用于定义过滤条件
     * @return 符合条件的类列表
     * @throws Exception 可能抛出的异常
     */
    public static List<Class<?>> getClassesWithFilter(String packageName, ClassFilter filter)
            throws Exception {
        List<Class<?>> filteredClasses = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                File directory = new File(resource.getFile());
                if (directory.exists()) {
                    findClassesInDirectory(directory, packageName, filter, filteredClasses);
                }
            } else if ("jar".equals(resource.getProtocol())) {
                findClassesInJar(resource.getPath(), packageName, filter, filteredClasses);
            }
        }
        return filteredClasses;
    }

    private static void findClassesInDirectory(
            File directory, String packageName, ClassFilter filter, List<Class<?>> filteredClasses)
            throws ClassNotFoundException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findClassesInDirectory(
                            file, packageName + "." + file.getName(), filter, filteredClasses);
                } else if (file.getName().endsWith(".class")) {
                    String className =
                            packageName
                                    + '.'
                                    + file.getName().substring(0, file.getName().length() - 6);
                    Class<?> clazz = Class.forName(className);
                    if (filter.matches(clazz)) {
                        filteredClasses.add(clazz);
                    }
                }
            }
        }
    }

    private static void findClassesInJar(
            String jarPath, String packageName, ClassFilter filter, List<Class<?>> filteredClasses)
            throws IOException, ClassNotFoundException {
        String packagePath = packageName.replace('.', '/');
        try (JarFile jarFile = new JarFile(jarPath.substring(0, jarPath.indexOf("!")))) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(packagePath) && entry.getName().endsWith(".class")) {
                    String className =
                            entry.getName()
                                    .replace('/', '.')
                                    .substring(0, entry.getName().length() - 6);
                    Class<?> clazz = Class.forName(className);
                    if (filter.matches(clazz)) {
                        filteredClasses.add(clazz);
                    }
                }
            }
        }
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
