package com.lidachui.simpleRequest.util;


import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 优化版本的注解参数提取工具类
 *
 * 优化点：
 * 1. 使用Spring的ConcurrentReferenceHashMap缓存反射结果，避免重复计算
 * 2. 使用Spring的ParameterNameDiscoverer获取参数名
 * 3. 预计算注解映射，减少运行时开销
 * 4. 使用更高效的数据结构
 */
public class AnnotationParamExtractorWithSpring {

    // 缓存方法的参数名称，避免重复反射
    // 使用Spring的ConcurrentReferenceHashMap，支持弱引用，自动清理
    private static final Map<Method, String[]> PARAMETER_NAMES_CACHE =
            new ConcurrentReferenceHashMap<>(256, ConcurrentReferenceHashMap.ReferenceType.WEAK);

    // 缓存方法的注解信息
    private static final Map<Method, Annotation[][]> PARAMETER_ANNOTATIONS_CACHE =
            new ConcurrentReferenceHashMap<>(256, ConcurrentReferenceHashMap.ReferenceType.WEAK);

    // Spring的参数名发现器，比原生反射更强大
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    // 预编译的注解处理器缓存
    private static final Map<Class<? extends Annotation>, Function<Annotation, String>>
            ANNOTATION_PROCESSORS = new ConcurrentHashMap<>();

    /**
     * 注册注解处理器
     *
     * @param annotationType 注解类型
     * @param keyExtractor 键值提取函数
     */
    public static void registerAnnotationProcessor(
            Class<? extends Annotation> annotationType,
            Function<Annotation, String> keyExtractor) {
        ANNOTATION_PROCESSORS.put(annotationType, keyExtractor);
    }

    /**
     * 提取带有指定注解的参数及其值（多注解类型版本）
     *
     * @param method 方法
     * @param args 参数值数组
     * @param annotationTypeMap 注解类型与提取键值函数的映射
     * @return 按注解类型分组的参数键值对
     */
    public static Map<Class<? extends Annotation>, Map<String, ParamInfo>> extractParamsWithTypes(
            Method method, Object[] args,
            Map<Class<? extends Annotation>, Function<Annotation, String>> annotationTypeMap) {

        if (method == null || args == null || annotationTypeMap == null || annotationTypeMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 初始化结果映射
        Map<Class<? extends Annotation>, Map<String, ParamInfo>> resultMap = new HashMap<>(annotationTypeMap.size());
        for (Class<? extends Annotation> annotationType : annotationTypeMap.keySet()) {
            resultMap.put(annotationType, new HashMap<>());
        }

        // 获取缓存的注解和参数名
        Annotation[][] parameterAnnotations = getParameterAnnotations(method);
        String[] parameterNames = getParameterNames(method);
        Class<?>[] parameterTypes = method.getParameterTypes();

        // 遍历参数
        for (int i = 0; i < Math.min(parameterAnnotations.length, args.length); i++) {
            Object argValue = args[i];
            if (argValue == null) {
                continue; // 跳过null值
            }

            // 处理当前参数的所有注解
            processParameterAnnotations(
                    parameterAnnotations[i],
                    annotationTypeMap,
                    parameterNames[i],
                    parameterTypes[i],
                    argValue,
                    resultMap
            );
        }

        return resultMap;
    }

    /**
     * 提取带有指定注解的参数及其值（单注解类型版本）
     *
     * @param method 方法
     * @param args 参数值数组
     * @param annotationType 注解类型
     * @param annotationKeyFunc 提取注解键值的方法
     * @return 参数键值对
     */
    public static Map<String, ParamInfo> extractParamsWithType(
            Method method, Object[] args,
            Class<? extends Annotation> annotationType,
            Function<Annotation, String> annotationKeyFunc) {

        if (method == null || args == null || annotationType == null || annotationKeyFunc == null) {
            return Collections.emptyMap();
        }

        Map<String, ParamInfo> params = new HashMap<>();
        Annotation[][] parameterAnnotations = getParameterAnnotations(method);
        String[] parameterNames = getParameterNames(method);
        Class<?>[] parameterTypes = method.getParameterTypes();

        for (int i = 0; i < Math.min(parameterAnnotations.length, args.length); i++) {
            Object argValue = args[i];
            if (argValue == null) {
                continue;
            }

            // 查找匹配的注解
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotationType.isInstance(annotation)) {
                    String key = extractKey(annotation, annotationKeyFunc, parameterNames[i]);
                    params.put(key, new ParamInfo(parameterTypes[i], argValue));
                    break; // 找到匹配的注解后跳出
                }
            }
        }

        return params;
    }

    /**
     * 使用预注册的处理器提取参数
     *
     * @param method 方法
     * @param args 参数值数组
     * @param annotationTypes 要处理的注解类型
     * @return 按注解类型分组的参数键值对
     */
    public static Map<Class<? extends Annotation>, Map<String, ParamInfo>> extractParamsWithRegisteredProcessors(
            Method method, Object[] args, Set<Class<? extends Annotation>> annotationTypes) {

        Map<Class<? extends Annotation>, Function<Annotation, String>> typeMap = new HashMap<>();
        for (Class<? extends Annotation> type : annotationTypes) {
            Function<Annotation, String> processor = ANNOTATION_PROCESSORS.get(type);
            if (processor != null) {
                typeMap.put(type, processor);
            }
        }

        return extractParamsWithTypes(method, args, typeMap);
    }

    /**
     * 处理参数的注解
     */
    private static void processParameterAnnotations(
            Annotation[] annotations,
            Map<Class<? extends Annotation>, Function<Annotation, String>> annotationTypeMap,
            String parameterName,
            Class<?> parameterType,
            Object argValue,
            Map<Class<? extends Annotation>, Map<String, ParamInfo>> resultMap) {

        for (Annotation annotation : annotations) {
            for (Map.Entry<Class<? extends Annotation>, Function<Annotation, String>> entry :
                    annotationTypeMap.entrySet()) {

                Class<? extends Annotation> annotationType = entry.getKey();
                Function<Annotation, String> keyExtractor = entry.getValue();

                if (annotationType.isInstance(annotation)) {
                    String key = extractKey(annotation, keyExtractor, parameterName);
                    resultMap.get(annotationType).put(key, new ParamInfo(parameterType, argValue));
                }
            }
        }
    }

    /**
     * 提取键值，如果注解值为空则使用参数名
     */
    private static String extractKey(Annotation annotation,
                                     Function<Annotation, String> keyExtractor,
                                     String parameterName) {
        String key = keyExtractor.apply(annotation);
        return (key == null || key.trim().isEmpty()) ? parameterName : key;
    }

    /**
     * 获取方法的参数名称（带缓存）
     */
    private static String[] getParameterNames(Method method) {
        return PARAMETER_NAMES_CACHE.computeIfAbsent(method, m -> {
            // 首先尝试使用Spring的参数名发现器
            String[] names = PARAMETER_NAME_DISCOVERER.getParameterNames(m);
            if (names != null) {
                return names;
            }

            // 回退到Java 8+的参数名反射
            return Arrays.stream(m.getParameters())
                    .map(param -> param.getName())
                    .toArray(String[]::new);
        });
    }

    /**
     * 获取方法的参数注解（带缓存）
     */
    private static Annotation[][] getParameterAnnotations(Method method) {
        return PARAMETER_ANNOTATIONS_CACHE.computeIfAbsent(method, Method::getParameterAnnotations);
    }

    /**
     * 清除缓存（用于测试或内存管理）
     */
    public static void clearCache() {
        PARAMETER_NAMES_CACHE.clear();
        PARAMETER_ANNOTATIONS_CACHE.clear();
    }

    /**
     * 获取缓存统计信息
     */
    public static String getCacheStats() {
        return String.format(
                "ParameterNames Cache size: %d, ParameterAnnotations Cache size: %d",
                PARAMETER_NAMES_CACHE.size(),
                PARAMETER_ANNOTATIONS_CACHE.size()
        );
    }
}