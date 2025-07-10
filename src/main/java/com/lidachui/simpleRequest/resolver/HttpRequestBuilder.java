package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.annotation.*;
import com.lidachui.simpleRequest.entity.QueryEntity;
import com.lidachui.simpleRequest.util.AnnotationParamExtractor;
import com.lidachui.simpleRequest.util.ImprovedPlaceholderReplacer;
import com.lidachui.simpleRequest.util.ParamInfo;
import com.lidachui.simpleRequest.util.RequestAnnotationParser;

import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DefaultRequestBuilder 构建 HTTP 请求，支持路径参数、查询参数和请求体
 *
 * @author: lihuijie
 * @date: 2024/11/21
 * @version: 1.1
 */
@Slf4j
public class HttpRequestBuilder implements RequestBuilder {

    private static final ImprovedPlaceholderReplacer placeholderReplacer =
            new ImprovedPlaceholderReplacer();

    // 简单类型集合，这些类型直接转换为字符串
    private static final Set<Class<?>> SIMPLE_TYPES = new HashSet<>(Arrays.asList(
            String.class, Integer.class, Long.class, Double.class, Float.class,
            Boolean.class, Character.class, Byte.class, Short.class,
            int.class, long.class, double.class, float.class,
            boolean.class, char.class, byte.class, short.class,
            BigDecimal.class, BigInteger.class,
            LocalDate.class, LocalDateTime.class, LocalTime.class,
            java.util.Date.class, java.sql.Date.class, java.sql.Timestamp.class
    ));

    @Override
    public Request buildRequest(Method method, Object[] args, Object... params) {
        String baseUrl = (String) params[0];

        RestRequest restRequest = method.getAnnotation(RestRequest.class);
        // 提取参数信息
        Map<Class<? extends Annotation>, Map<String, ParamInfo>> methodParams =
                getParams(method, args);

        // 构建 URL 和 Query 参数
        List<QueryEntity> queryEntities = new ArrayList<>();
        Pair<String, Map<String, String>> urlPair =
                constructUrl(baseUrl, restRequest, methodParams, queryEntities);
        String fullUrl = urlPair.getKey();
        // 构建 Header 参数
        Map<String, String> headers = constructHeaders(restRequest.headers(), methodParams);

        // 提取 Body 参数
        Object body = extractBodyParam(methodParams);

        // 构建请求对象
        Request request = new Request();
        request.setUrl(fullUrl);
        request.setMethod(restRequest.method());
        request.setHeaders(headers);
        request.setBody(body);
        request.setQueryParams(urlPair.getValue());
        request.setQueryEntities(queryEntities);
        logRequestDetails(request);
        return request;
    }

    private Map<Class<? extends Annotation>, Map<String, ParamInfo>> getParams(
            Method method, Object[] args) {

        Map<Class<? extends Annotation>, Function<Annotation, String>> annotationTypeMap =
                new HashMap<>();
        annotationTypeMap.put(
                PathVariable.class, annotation -> ((PathVariable) annotation).value());
        annotationTypeMap.put(QueryParam.class, annotation -> ((QueryParam) annotation).value());
        annotationTypeMap.put(HeaderParam.class, annotation -> ((HeaderParam) annotation).value());
        annotationTypeMap.put(BodyParam.class, annotation -> "body");
        annotationTypeMap.put(Host.class, annotation -> "host");

        Map<Class<? extends Annotation>, Map<String, ParamInfo>> result =
                AnnotationParamExtractor.extractParamsWithTypes(method, args, annotationTypeMap);

        // 处理没有注解的参数，将其作为QueryParam处理
        handleUnannotatedParams(method, args, result);

        return result;
    }

    /**
     * 处理没有注解的参数，将其作为QueryParam处理
     */
    private void handleUnannotatedParams(Method method, Object[] args,
            Map<Class<? extends Annotation>, Map<String, ParamInfo>> result) {

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Map<String, ParamInfo> queryParams = result.computeIfAbsent(QueryParam.class, k -> new HashMap<>());

        for (int i = 0; i < parameters.length && i < args.length; i++) {
            java.lang.reflect.Parameter parameter = parameters[i];

            // 检查参数是否已经有注解处理了
            boolean hasAnnotation = parameter.isAnnotationPresent(PathVariable.class) ||
                                  parameter.isAnnotationPresent(QueryParam.class) ||
                                  parameter.isAnnotationPresent(HeaderParam.class) ||
                                  parameter.isAnnotationPresent(BodyParam.class) ||
                                  parameter.isAnnotationPresent(Host.class);

            if (!hasAnnotation && args[i] != null) {
                String paramName = parameter.getName();
                ParamInfo paramInfo = new ParamInfo(parameter.getType(), args[i]);
                queryParams.put(paramName, paramInfo);
            }
        }
    }

    private Pair<String, Map<String, String>> constructUrl(
            String baseUrl,
            RestRequest restRequest,
            Map<Class<? extends Annotation>, Map<String, ParamInfo>> params,
            List<QueryEntity> queryEntities) {

        // 替换路径变量
        Set<String> consumedKeys = new HashSet<>();
        String path =
                placeholderReplacer.replacePlaceholdersWithDefaults(
                        restRequest.path(),
                        params.getOrDefault(PathVariable.class, Collections.emptyMap()),
                        consumedKeys);

        // 解析注解上的 Query 参数并合并动态参数
        Map<String, String> queryParams =
                mergeQueryParams(
                        restRequest.queryParams(),
                        params.getOrDefault(QueryParam.class, Collections.emptyMap()),
                        consumedKeys,
                        queryEntities);

        Map<String, ParamInfo> hostParams = params.getOrDefault(Host.class, Collections.emptyMap());
        AtomicReference<String> host = new AtomicReference<>(baseUrl);
        if (!hostParams.isEmpty()) {
            Optional<ParamInfo> first = hostParams.values().stream().findFirst();
            first.ifPresent(
                    paramInfo -> {
                        String newHost = paramInfo.getValue().toString();
                        host.set(newHost);
                    });
        }
        // 构建完整 URL
        return new Pair<>(buildUrl(host.get(), path, queryEntities), queryParams);
    }

    private Map<String, String> mergeQueryParams(
            String[] staticQueryParams,
            Map<String, ParamInfo> dynamicParams,
            Set<String> consumedKeys,
            List<QueryEntity> queryEntities) {

        // 解析静态 Query 参数
        Map<String, String> queryParams =
                RequestAnnotationParser.parseQueryParams(staticQueryParams);

        // 替换占位符
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            String value = entry.getValue();

            // 替换 value 的占位符
            String replacedValue = placeholderReplacer.replacePlaceholders(value, dynamicParams, consumedKeys);

            // 更新 map 中的 value
            entry.setValue(replacedValue);
            queryEntities.add(new QueryEntity(entry.getKey(), entry.getValue()));
        }

        // 添加未消费的动态参数
        dynamicParams.forEach(
                (key, paramInfo) -> {
                    if (!consumedKeys.contains(key)) {
                        Object value = paramInfo.getValue();

                        if (value instanceof Collection) {
                            Collection<?> collection = (Collection<?>) value;

                            if (!collection.isEmpty()) {
                                collection.forEach(
                                        item -> {
                                            String stringValue = item.toString();
                                            queryParams.putIfAbsent(key, stringValue);

                                            QueryEntity queryEntity = new QueryEntity();
                                            queryEntity.setName(key);
                                            queryEntity.setValue(stringValue);
                                            queryEntities.add(queryEntity);
                                        });
                            } else {
                                queryParams.putIfAbsent(key, "");
                            }
                        } else if (isSimpleType(value)) {
                            // 简单类型直接转换
                            String stringValue = value.toString();
                            queryParams.putIfAbsent(key, stringValue);

                            QueryEntity queryEntity = new QueryEntity();
                            queryEntity.setName(key);
                            queryEntity.setValue(stringValue);
                            queryEntities.add(queryEntity);
                        } else {
                            // 使用Spring的BeanWrapper解析实体类对象
                            Map<String, Object> entityFields = extractEntityFieldsUsingSpring(value);
                            entityFields.forEach((fieldName, fieldValue) -> {
                                if (fieldValue != null) {
                                    String stringValue = fieldValue.toString();
                                    queryParams.putIfAbsent(fieldName, stringValue);

                                    QueryEntity queryEntity = new QueryEntity();
                                    queryEntity.setName(fieldName);
                                    queryEntity.setValue(stringValue);
                                    queryEntities.add(queryEntity);
                                }
                            });
                        }
                    }
                });

        return queryParams;
    }

    /**
     * 检查对象是否为简单类型
     */
    private boolean isSimpleType(Object value) {
        if (value == null) {
            return true;
        }

        Class<?> clazz = value.getClass();

        // 使用Spring的BeanUtils判断简单类型
        if (BeanUtils.isSimpleProperty(clazz)) {
            return true;
        }

        // 额外检查一些Spring没有包含的类型
        if (SIMPLE_TYPES.contains(clazz)) {
            return true;
        }

        // 检查是否为枚举
        return clazz.isEnum();
    }

    /**
     * 使用Spring的BeanWrapper提取实体类的字段
     */
    private Map<String, Object> extractEntityFieldsUsingSpring(Object entity) {
        Map<String, Object> fields = new HashMap<>();

        if (entity == null) {
            return fields;
        }

        try {
            // 使用Spring的BeanWrapper来处理Bean属性
            BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
            PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();

            for (PropertyDescriptor pd : propertyDescriptors) {
                String propertyName = pd.getName();

                // 跳过class属性和只写属性
                if ("class".equals(propertyName) || pd.getReadMethod() == null) {
                    continue;
                }

                try {
                    Object value = beanWrapper.getPropertyValue(propertyName);

                    if (value != null) {
                        if (isSimpleType(value)) {
                            fields.put(propertyName, value);
                        } else if (value instanceof Collection) {
                            // 集合类型处理
                            Collection<?> collection = (Collection<?>) value;
                            if (!collection.isEmpty()) {
                                // 检查集合中的元素是否为简单类型
                                Object firstElement = collection.iterator().next();
                                if (isSimpleType(firstElement)) {
                                    // 如果是简单类型的集合，用逗号分隔
                                    String joinedValue = collection.stream()
                                            .filter(Objects::nonNull)
                                            .map(Object::toString)
                                            .collect(Collectors.joining(","));
                                    if (StringUtils.hasText(joinedValue)) {
                                        fields.put(propertyName, joinedValue);
                                    }
                                }
                            }
                        } else if (value instanceof Map) {
                            // Map类型暂不处理，避免复杂性
                            log.debug("跳过Map类型属性: {}", propertyName);
                        }
                        // 嵌套对象暂不处理，避免无限递归
                    }
                } catch (Exception e) {
                    log.warn("无法访问属性: {}, 错误: {}", propertyName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("使用BeanWrapper解析实体类失败: {}", e.getMessage(), e);
            // 如果Spring解析失败，返回空Map而不是抛异常
        }

        return fields;
    }

    private Map<String, String> constructHeaders(
            String[] staticHeaders,
            Map<Class<? extends Annotation>, Map<String, ParamInfo>> params) {

        // 解析静态 Header 参数
        Map<String, String> headers = RequestAnnotationParser.parseHeaders(staticHeaders);

        // 动态替换 Header 参数
        Map<String, ParamInfo> headerParams =
                params.getOrDefault(HeaderParam.class, Collections.emptyMap());
        Set<String> consumedKeys = new HashSet<>();
        headers.replaceAll((key, value) -> placeholderReplacer.replacePlaceholdersWithDefaults(value, headerParams, consumedKeys));

        // 添加未消费的动态 Header 参数
        headerParams.forEach(
                (key, paramInfo) -> {
                    if (!consumedKeys.contains(key)) {
                        headers.putIfAbsent(key, paramInfo.getValue().toString());
                    }
                });

        return headers;
    }

    private Object extractBodyParam(
            Map<Class<? extends Annotation>, Map<String, ParamInfo>> params) {
        Map<String, ParamInfo> bodyParams =
                params.getOrDefault(BodyParam.class, Collections.emptyMap());
        return bodyParams.isEmpty() ? null : bodyParams.values().iterator().next().getValue();
    }

    private String buildUrl(String baseUrl, String path, List<QueryEntity> queryEntities) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl).append(path);
        if (!queryEntities.isEmpty()) {
            urlBuilder.append("?");
            // 使用 queryEntities 来构建查询参数
            urlBuilder.append(
                    queryEntities.stream()
                            .map(entity -> entity.getName() + "=" + entity.getValue())
                            .collect(Collectors.joining("&")));
        }
        return urlBuilder.toString();
    }

    private void logRequestDetails(Request request) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Built request: URL={}, Method={}, Headers={}, Body={}",
                    request.getUrl(),
                    request.getMethod(),
                    request.getHeaders(),
                    request.getBody());
        }
    }
}
