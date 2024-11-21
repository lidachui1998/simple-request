package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.annotation.BodyParam;
import com.lidachui.simpleRequest.annotation.HeaderParam;
import com.lidachui.simpleRequest.annotation.QueryParam;
import com.lidachui.simpleRequest.annotation.RestRequest;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * DefaultRequestBuilder
 *
 * @author: lihuijie
 * @date: 2024/11/21 11:32
 * @version: 1.0
 */
@Slf4j
public class DefaultRequestBuilder implements RequestBuilder {

    @Override
    public Request buildRequest(
            Method method, Object[] args, String baseUrl, RestRequest restRequest) {
        Request request = new Request();

        // 构建 URL
        String fullUrl = buildFullUrl(baseUrl, restRequest.path(), method, args, restRequest);
        request.setUrl(fullUrl);

        // 设置 HTTP 方法
        request.setMethod(restRequest.method());

        // 构建 Headers
        Map<String, String> headers = parseHeaders(restRequest.headers(), method, args);
        request.setHeaders(headers);

        // 提取 Body 参数
        Object body = extractBodyParam(method, args);
        request.setBody(body);

        log.debug("Built request: {}", request);
        return request;
    }

    /**
     * 构建完整url
     *
     * @param baseUrl 基本url
     * @param path 路径
     * @param method 方法
     * @param args args
     * @param restRequest 休息请求
     * @return {@code String }
     */
    private String buildFullUrl(
            String baseUrl, String path, Method method, Object[] args, RestRequest restRequest) {
        StringBuilder fullUrlBuilder = new StringBuilder(baseUrl);
        fullUrlBuilder.append(replacePathVariables(path, method, args));

        Map<String, String> queryParams = buildQueryMap(restRequest.queryParams(), method, args);
        if ( queryParams != null && !queryParams.isEmpty()) {
            fullUrlBuilder
                    .append("?")
                    .append(
                            queryParams.entrySet().stream()
                                    .map(
                                            entry ->
                                                    encode(entry.getKey())
                                                            + "="
                                                            + encode(entry.getValue()))
                                    .collect(Collectors.joining("&")));
        }

        return fullUrlBuilder.toString();
    }

    // URL 编码方法，确保参数安全
    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode URL parameter: " + value, e);
        }
    }

    private String replacePathVariables(String path, Method method, Object[] args) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof QueryParam) {
                    String paramName = ((QueryParam) annotation).value();
                    path = path.replace("{" + paramName + "}", args[i].toString());
                }
            }
        }
        return path;
    }

    private Map<String, String> buildQueryMap(String[] queryParams, Method method, Object[] args) {
        Map<String, String> queryMap = parseKeyValuePairs(queryParams);
        queryMap.putAll(extractMethodParams(method, args));
        return queryMap;
    }

    private Map<String, String> parseHeaders(String[] headers, Method method, Object[] args) {
        Map<String, String> headerMap = new HashMap<>();
        for (String header : headers) {
            String[] split = header.split(":", 2);
            if (split.length == 2) {
                String key = split[0].trim();
                String value = split[1].trim();
                headerMap.put(key, resolveHeaderParamValue(value, method, args));
            }
        }
        return headerMap;
    }

    private Map<String, String> parseKeyValuePairs(String[] keyValuePairs) {
        Map<String, String> map = new HashMap<>();
        for (String pair : keyValuePairs) {
            String[] split = pair.split(":", 2);
            if (split.length == 2) {
                map.put(split[0].trim(), split[1].trim());
            }
        }
        return map;
    }

    private Map<String, String> extractMethodParams(Method method, Object[] args) {
        Map<String, String> params = new HashMap<>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof QueryParam) {
                    String paramName = ((QueryParam) annotation).value();
                    params.put(paramName, args[i].toString());
                }
            }
        }
        return params;
    }

    private Object extractBodyParam(Method method, Object[] args) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof BodyParam) {
                    return args[i];
                }
            }
        }
        return null;
    }

    private String resolveHeaderParamValue(String value, Method method, Object[] args) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < args.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof HeaderParam) {
                    String paramName = ((HeaderParam) annotation).value();
                    if (value.contains("{" + paramName + "}")) {
                        value = value.replace("{" + paramName + "}", args[i].toString());
                    }
                }
            }
        }
        return value;
    }
}
