package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.annotation.*;
import com.lidachui.simpleRequest.entity.QueryEntity;
import com.lidachui.simpleRequest.util.AnnotationParamExtractor;
import com.lidachui.simpleRequest.util.ParamInfo;
import com.lidachui.simpleRequest.util.RequestAnnotationParser;

import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        return AnnotationParamExtractor.extractParamsWithTypes(method, args, annotationTypeMap);
    }

    private Pair<String, Map<String, String>> constructUrl(
            String baseUrl,
            RestRequest restRequest,
            Map<Class<? extends Annotation>, Map<String, ParamInfo>> params,
            List<QueryEntity> queryEntities) {

        // 替换路径变量
        Set<String> consumedKeys = new HashSet<>();
        String path =
                replacePlaceholders(
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
            String replacedValue = replacePlaceholders(value, dynamicParams, consumedKeys);

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
                        } else if (value != null) {
                            String stringValue = value.toString();
                            queryParams.putIfAbsent(key, stringValue);

                            QueryEntity queryEntity = new QueryEntity();
                            queryEntity.setName(key);
                            queryEntity.setValue(stringValue);
                            queryEntities.add(queryEntity);
                        }
                    }
                });

        return queryParams;
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
        headers.replaceAll((key, value) -> replacePlaceholders(value, headerParams, consumedKeys));

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

    private String replacePlaceholders(
            String template, Map<String, ParamInfo> params, Set<String> consumedKeys) {

        if (template == null || params.isEmpty()) {
            return template;
        }

        StringBuilder result = new StringBuilder();
        int cursor = 0;

        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)\\}");
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            result.append(template, cursor, matcher.start());
            String key = matcher.group(1);
            String replacement = "";
            if (params.containsKey(key)) {
                ParamInfo paramInfo = params.get(key);
                Object value = paramInfo.getValue();

                if (value instanceof Collection) {
                    Collection<?> collection = (Collection<?>) value;
                    if (!collection.isEmpty()) {
                        replacement = collection.iterator().next().toString();
                    } else {
                        replacement = ""; // 或者根据需要处理空集合
                    }
                } else if (value != null) {
                    replacement = value.toString();
                } else {
                    replacement = ""; // 处理 null 值的情况
                }
            } else {
                replacement = matcher.group();
            }

            result.append(replacement);

            if (params.containsKey(key)) {
                consumedKeys.add(key);
            }
            cursor = matcher.end();
        }
        result.append(template, cursor, template.length());

        return result.toString();
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
