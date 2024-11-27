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

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        String fullUrl =
                buildFullUrl(
                        baseUrl,
                        restRequest.path(),
                        restRequest.queryParams(),
                        params.getOrDefault(PathVariable.class, Collections.emptyMap()),
                        params.getOrDefault(QueryParam.class, Collections.emptyMap()));

        // 构建 Header 参数
        Map<String, String> headers =
                parseHeaders(
                        restRequest.headers(),
                        params.getOrDefault(HeaderParam.class, Collections.emptyMap()));

        // 提取 Body 参数
        Object body =
                extractBodyParam(params.getOrDefault(BodyParam.class, Collections.emptyMap()));

        // 构建请求对象
        Request request = new Request();
        request.setUrl(fullUrl);
        request.setMethod(restRequest.method());
        request.setHeaders(headers);
        request.setBody(body);
        if (log.isDebugEnabled()) {
            log.debug(
                    "Built request: URL={}, Method={}, Headers={}, Body={}",
                    request.getUrl(),
                    request.getMethod(),
                    request.getHeaders(),
                    request.getBody());
        }
        return request;
    }

    private static Map<Class<? extends Annotation>, Map<String, ParamInfo>> getParams(
            Method method, Object[] args) {
        Map<Class<? extends Annotation>, Function<Annotation, String>> annotationTypeMap =
                new HashMap<>();
        annotationTypeMap.put(
                PathVariable.class, annotation -> ((PathVariable) annotation).value());
        annotationTypeMap.put(QueryParam.class, annotation -> ((QueryParam) annotation).value());
        annotationTypeMap.put(HeaderParam.class, annotation -> ((HeaderParam) annotation).value());
        annotationTypeMap.put(BodyParam.class, annotation -> "body");

        return AnnotationParamExtractor.extractParamsWithTypes(method, args, annotationTypeMap);
    }

    private String buildFullUrl(
            String baseUrl,
            String path,
            String[] queryParams,
            Map<String, ParamInfo> pathParams,
            Map<String, ParamInfo> methodQueryParams) {

        // 替换路径变量
        Set<String> consumedKeys = new HashSet<>();
        path = replacePlaceholders(path, pathParams, consumedKeys);

        // 解析注解上的 Query 参数
        Map<String, String> annotationQueryParams =
                RequestAnnotationParser.parseQueryParams(queryParams);

        // 替换 Query 参数中的占位符
        annotationQueryParams.replaceAll(
                (key, value) -> replacePlaceholders(value, methodQueryParams, consumedKeys));

        // 添加未被消费的动态 Query 参数
        methodQueryParams.forEach(
                (key, paramInfo) -> {
                    if (!consumedKeys.contains(key)) {
                        annotationQueryParams.putIfAbsent(key, paramInfo.getValue().toString());
                    }
                });

        // 构建完整的 URL
        StringBuilder fullUrlBuilder = new StringBuilder(baseUrl).append(path);
        if (!annotationQueryParams.isEmpty()) {
            fullUrlBuilder
                    .append("?")
                    .append(
                            annotationQueryParams.entrySet().stream()
                                    .map(
                                            entry ->
                                                    encode(entry.getKey())
                                                            + "="
                                                            + encode(entry.getValue()))
                                    .collect(Collectors.joining("&")));
        }
        return fullUrlBuilder.toString();
    }

    private Map<String, String> parseHeaders(
            String[] headers, Map<String, ParamInfo> headerParams) {
        Map<String, String> staticHeaders = RequestAnnotationParser.parseHeaders(headers);

        Set<String> consumedKeys = new HashSet<>();
        staticHeaders.replaceAll(
                (key, value) -> replacePlaceholders(value, headerParams, consumedKeys));

        // 添加未被消费的动态 Header 参数
        headerParams.forEach(
                (key, paramInfo) -> {
                    if (!consumedKeys.contains(key)) {
                        staticHeaders.putIfAbsent(key, paramInfo.getValue().toString());
                    }
                });

        return staticHeaders;
    }

    private Object extractBodyParam(Map<String, ParamInfo> bodyParams) {
        return bodyParams.isEmpty() ? null : bodyParams.values().iterator().next().getValue();
    }

    private String replacePlaceholders(
            String template, Map<String, ParamInfo> params, Set<String> consumedKeys) {
        if (template == null || params.isEmpty()) {
            return template;
        }

        // 1. 将参数 Map 提前转换为一个简单的 Map<String, String>，减少每次获取值时的复杂操作
        Map<String, String> paramValues = new HashMap<>();
        for (Map.Entry<String, ParamInfo> entry : params.entrySet()) {
            paramValues.put(entry.getKey(), entry.getValue().getValue().toString());
        }

        // 2. 使用正则表达式进行批量替换
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)\\}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1); // 获取 key 部分，即 aa
            String replacement = paramValues.get(key); // 获取替换值

            if (replacement != null) {
                // 直接替换
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                consumedKeys.add(key); // 标记为已消费
            } else {
                // 如果没有对应的参数，保留原占位符
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }

        // 将剩余的未匹配部分追加到结果中
        matcher.appendTail(result);

        return result.toString();
    }

    private String encode(String value) {
        // 只做一次编码并缓存，避免重复调用
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode URL parameter: " + value, e);
        }
    }
}
