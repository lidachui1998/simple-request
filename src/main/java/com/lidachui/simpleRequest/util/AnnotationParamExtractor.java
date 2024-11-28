package com.lidachui.simpleRequest.util;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

/**
 * AnnotationParamExtractor
 *
 * @author: lihuijie
 * @date: 2024/11/23 11:50
 * @version: 1.0
 */
public class AnnotationParamExtractor {

    /**
     * 提取带有指定注解的参数及其值。
     *
     * @param method             方法
     * @param args               参数值数组
     * @param annotationTypeMap  注解类型与提取键值函数的映射
     * @return 参数键值对，键为注解的值或参数名，值为参数信息（包括类型和值）
     */
    public static Map<Class<? extends Annotation>, Map<String, ParamInfo>> extractParamsWithTypes(
            Method method, Object[] args,
            Map<Class<? extends Annotation>, Function<Annotation, String>> annotationTypeMap) {

        Map<Class<? extends Annotation>, Map<String, ParamInfo>> resultMap = new HashMap<>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        String[] parameterNames = getParameterNames(method); // 获取参数名称

        for (Class<? extends Annotation> annotationType : annotationTypeMap.keySet()) {
            resultMap.put(annotationType, new HashMap<>());
        }

        for (int i = 0; i < parameterAnnotations.length; i++) {
            if (args[i] == null) {
                continue; // 跳过值为 null 的参数
            }

            for (Annotation annotation : parameterAnnotations[i]) {
                for (Map.Entry<Class<? extends Annotation>, Function<Annotation, String>> entry :
                        annotationTypeMap.entrySet()) {
                    Class<? extends Annotation> annotationType = entry.getKey();
                    Function<Annotation, String> keyExtractor = entry.getValue();

                    if (annotationType.isInstance(annotation)) {
                        // 如果注解值为空字符串，则使用参数名称作为键
                        String key = keyExtractor.apply(annotation);
                        if (key == null || key.trim().isEmpty()) {
                            key = parameterNames[i]; // 使用参数名称
                        }

                        // 添加到对应的注解类型映射
                        resultMap.get(annotationType)
                                .put(key, new ParamInfo(method.getParameterTypes()[i], args[i]));
                    }
                }
            }
        }
        return resultMap;
    }

    /**
     * 提取带有指定注解的参数及其值。
     *
     * @param method            方法
     * @param args              参数值数组
     * @param annotationType    注解类型
     * @param annotationKeyFunc 提取注解键值的方法（如果注解值为空则使用参数名）
     * @return 参数键值对，键为注解的值或参数名，值为参数信息（包括类型和值）
     */
    public static Map<String, ParamInfo> extractParamsWithType(Method method, Object[] args,
                                                               Class<? extends Annotation> annotationType,
                                                               Function<Annotation, String> annotationKeyFunc) {
        Map<String, ParamInfo> params = new HashMap<>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        String[] parameterNames = getParameterNames(method); // 获取参数名称

        for (int i = 0; i < parameterAnnotations.length; i++) {
            if (args[i] == null) {
                continue; // 跳过值为 null 的参数
            }

            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotationType.isInstance(annotation)) {
                    // 如果注解值为空字符串，则使用参数名称作为键
                    String key = annotationKeyFunc.apply(annotation);
                    if (key == null || key.trim().isEmpty()) {
                        key = parameterNames[i]; // 使用参数名称
                    }
                    params.put(key, new ParamInfo(method.getParameterTypes()[i], args[i]));
                }
            }
        }
        return params;
    }

    /**
     * 获取方法的参数名称。
     *
     * @param method 方法
     * @return 参数名称数组
     */
    private static String[] getParameterNames(Method method) {
        // 使用反射或编译工具（如 javac 参数 `-parameters`）来获取参数名称
        // 示例代码假设 `-parameters` 参数已启用
        return method.getParameters() == null ? new String[0] :
                java.util.Arrays.stream(method.getParameters())
                        .map(java.lang.reflect.Parameter::getName)
                        .toArray(String[]::new);
    }
}
