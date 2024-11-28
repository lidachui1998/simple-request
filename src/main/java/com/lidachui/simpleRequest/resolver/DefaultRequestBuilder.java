package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.annotation.BodyParam;
import com.lidachui.simpleRequest.annotation.HeaderParam;
import com.lidachui.simpleRequest.annotation.PathVariable;
import com.lidachui.simpleRequest.annotation.QueryParam;
import com.lidachui.simpleRequest.annotation.RestRequest;
import com.lidachui.simpleRequest.util.AnnotationParamExtractor;
import com.lidachui.simpleRequest.util.ParamInfo;
import com.lidachui.simpleRequest.util.RequestAnnotationParser;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
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
public class DefaultRequestBuilder implements RequestBuilder {

    @Override
    public Request buildRequest(
            Method method, Object[] args, String baseUrl, RestRequest restRequest) {

        // 提取参数信息
        Map<Class<? extends Annotation>, Map<String, ParamInfo>> params = getParams(method, args);

        // 构建 URL 和 Query 参数
        String fullUrl = constructUrl(baseUrl, restRequest, params);

        // 构建 Header 参数
        Map<String, String> headers = constructHeaders(restRequest.headers(), params);

        // 提取 Body 参数
        Object body = extractBodyParam(params);

        // 构建请求对象
        Request request = new Request();
        request.setUrl(fullUrl);
        request.setMethod(restRequest.method());
        request.setHeaders(headers);
        request.setBody(body);

        logRequestDetails(request);
        return request;
    }

    private Map<Class<? extends Annotation>, Map<String, ParamInfo>> getParams(
            Method method, Object[] args) {

        Map<Class<? extends Annotation>, Function<Annotation, String>> annotationTypeMap = new HashMap<>();
        annotationTypeMap.put(PathVariable.class, annotation -> ((PathVariable) annotation).value());
        annotationTypeMap.put(QueryParam.class, annotation -> ((QueryParam) annotation).value());
        annotationTypeMap.put(HeaderParam.class, annotation -> ((HeaderParam) annotation).value());
        annotationTypeMap.put(BodyParam.class, annotation -> "body");

        return AnnotationParamExtractor.extractParamsWithTypes(method, args, annotationTypeMap);
    }

    private String constructUrl(String baseUrl, RestRequest restRequest,
                                Map<Class<? extends Annotation>, Map<String, ParamInfo>> params) {

        // 替换路径变量
        Set<String> consumedKeys = new HashSet<>();
        String path = replacePlaceholders(
                restRequest.path(),
                params.getOrDefault(PathVariable.class, Collections.emptyMap()),
                consumedKeys);

        // 解析注解上的 Query 参数并合并动态参数
        Map<String, String> queryParams = mergeQueryParams(
                restRequest.queryParams(),
                params.getOrDefault(QueryParam.class, Collections.emptyMap()),
                consumedKeys);

        // 构建完整 URL
        return buildUrl(baseUrl, path, queryParams);
    }

    private Map<String, String> mergeQueryParams(
            String[] staticQueryParams, Map<String, ParamInfo> dynamicParams, Set<String> consumedKeys) {

        // 解析静态 Query 参数
        Map<String, String> queryParams = RequestAnnotationParser.parseQueryParams(staticQueryParams);

        // 替换占位符
        queryParams.replaceAll((key, value) -> replacePlaceholders(value, dynamicParams, consumedKeys));

        // 添加未消费的动态参数
        dynamicParams.forEach((key, paramInfo) -> {
            if (!consumedKeys.contains(key)) {
                queryParams.putIfAbsent(key, paramInfo.getValue().toString());
            }
        });

        return queryParams;
    }

    private Map<String, String> constructHeaders(
            String[] staticHeaders, Map<Class<? extends Annotation>, Map<String, ParamInfo>> params) {

        // 解析静态 Header 参数
        Map<String, String> headers = RequestAnnotationParser.parseHeaders(staticHeaders);

        // 动态替换 Header 参数
        Map<String, ParamInfo> headerParams = params.getOrDefault(HeaderParam.class, Collections.emptyMap());
        Set<String> consumedKeys = new HashSet<>();
        headers.replaceAll((key, value) -> replacePlaceholders(value, headerParams, consumedKeys));

        // 添加未消费的动态 Header 参数
        headerParams.forEach((key, paramInfo) -> {
            if (!consumedKeys.contains(key)) {
                headers.putIfAbsent(key, paramInfo.getValue().toString());
            }
        });

        return headers;
    }

    private Object extractBodyParam(Map<Class<? extends Annotation>, Map<String, ParamInfo>> params) {
        Map<String, ParamInfo> bodyParams = params.getOrDefault(BodyParam.class, Collections.emptyMap());
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
            String replacement = params.containsKey(key) ? params.get(key).getValue().toString() : matcher.group();
            result.append(replacement);

            if (params.containsKey(key)) {
                consumedKeys.add(key);
            }
            cursor = matcher.end();
        }
        result.append(template, cursor, template.length());

        return result.toString();
    }

    private String buildUrl(String baseUrl, String path, Map<String, String> queryParams) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl).append(path);
        if (!queryParams.isEmpty()) {
            urlBuilder.append("?")
                    .append(queryParams.entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
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
