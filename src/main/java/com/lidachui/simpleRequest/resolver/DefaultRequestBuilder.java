package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.annotation.BodyParam;
import com.lidachui.simpleRequest.annotation.HeaderParam;
import com.lidachui.simpleRequest.annotation.PathVariable;
import com.lidachui.simpleRequest.annotation.QueryParam;
import com.lidachui.simpleRequest.annotation.RestRequest;
import com.lidachui.simpleRequest.util.AnnotationParamExtractor;
import com.lidachui.simpleRequest.util.ParamInfo;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
public class DefaultRequestBuilder implements RequestBuilder {

    /**
     * 构建请求
     *
     * @param method 方法
     * @param args 方法参数值
     * @param baseUrl 基本 URL
     * @param restRequest RestRequest 注解
     * @return 构建好的 Request 对象
     */
    @Override
    public Request buildRequest(
            Method method, Object[] args, String baseUrl, RestRequest restRequest) {
        Request request = new Request();

        Map<Class<? extends Annotation>, Map<String, ParamInfo>> params = getParams(method, args);

        // 获取不同类型注解参数
        Map<String, ParamInfo> pathParams = params.get(PathVariable.class);
        Map<String, ParamInfo> queryParams = params.get(QueryParam.class);
        Map<String, ParamInfo> headerParams = params.get(HeaderParam.class);
        Map<String, ParamInfo> bodyParams = params.get(BodyParam.class);

        // 构建完整 URL
        String fullUrl = buildFullUrl(baseUrl, restRequest.path(), method, pathParams, queryParams);
        request.setUrl(fullUrl);

        // 设置 HTTP 方法
        request.setMethod(restRequest.method());

        // 构建 Headers
        Map<String, String> headers = parseHeaders(restRequest.headers(), headerParams);
        request.setHeaders(headers);

        // 提取 Body 参数
        Object body = extractBodyParam(bodyParams);
        request.setBody(body);

        // 设置响应类型
        request.setResponseType(method.getReturnType());

        log.debug("Built request: {}", request);
        return request;
    }

    @NotNull
    private static Map<Class<? extends Annotation>, Map<String, ParamInfo>> getParams(
            Method method, Object[] args) {
        Map<Class<? extends Annotation>, Function<Annotation, String>> annotationTypeMap =
                new HashMap<>();
        annotationTypeMap.put(
                PathVariable.class, annotation -> ((PathVariable) annotation).value());
        annotationTypeMap.put(QueryParam.class, annotation -> ((QueryParam) annotation).value());
        annotationTypeMap.put(HeaderParam.class, annotation -> ((HeaderParam) annotation).value());
        annotationTypeMap.put(BodyParam.class, annotation -> "body"); // 默认键值

        return AnnotationParamExtractor.extractParamsWithTypes(method, args, annotationTypeMap);
    }

    private String buildFullUrl(
            String baseUrl,
            String path,
            Method method,
            Map<String, ParamInfo> pathParams,
            Map<String, ParamInfo> methodQueryParams) {
        StringBuilder fullUrlBuilder = new StringBuilder(baseUrl);

        // 替换路径参数
        path = replacePathVariables(path, pathParams);
        fullUrlBuilder.append(path);

        // 从注解中提取 Query 参数
        Map<String, String> annotationQueryParams = parseQueryParamsFromAnnotation(method);

        Map<String, String> methodQueryParamsMap =
                methodQueryParams.entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().getValue().toString()));

        // 替换注解中 Query 参数的占位符
        annotationQueryParams.replaceAll(
                (key, value) -> {
                    if (value.contains("{")) { // 如果存在占位符
                        for (Map.Entry<String, String> entry : methodQueryParamsMap.entrySet()) {
                            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
                        }
                    }
                    return value;
                });

        // 合并 Query 参数，方法参数优先
        methodQueryParamsMap.forEach(annotationQueryParams::putIfAbsent);

        // 构建完整 URL 的 Query 部分
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

    private Map<String, String> parseQueryParamsFromAnnotation(Method method) {
        RestRequest restRequest = method.getAnnotation(RestRequest.class);
        Map<String, String> queryParams = new HashMap<>();

        if (restRequest != null) {
            String[] queryParamArray = restRequest.queryParams();
            for (String queryParam : queryParamArray) {
                String[] split = queryParam.split("=", 2);
                if (split.length == 2) {
                    queryParams.put(split[0].trim(), split[1].trim());
                }
            }
        }
        return queryParams;
    }

    private String replacePathVariables(String path, Map<String, ParamInfo> pathParams) {
        for (Map.Entry<String, ParamInfo> entry : pathParams.entrySet()) {
            if (path.contains("{" + entry.getKey() + "}")) {
                path =
                        path.replace(
                                "{" + entry.getKey() + "}",
                                encode(entry.getValue().getValue().toString()));
            }
        }
        return path;
    }

    private Map<String, String> parseHeaders(
            String[] headers, Map<String, ParamInfo> headerParams) {
        Map<String, String> headerMap = new HashMap<>();
        for (String header : headers) {
            String[] split = header.split(":", 2);
            if (split.length == 2) {
                String key = split[0].trim();
                String value = split[1].trim();
                headerMap.put(key, resolveHeaderParamValue(value, headerParams));
            }
        }
        return headerMap;
    }

    private String resolveHeaderParamValue(String value, Map<String, ParamInfo> headerParams) {
        for (Map.Entry<String, ParamInfo> entry : headerParams.entrySet()) {
            if (value.contains("{" + entry.getKey() + "}")) {
                value =
                        value.replace(
                                "{" + entry.getKey() + "}", entry.getValue().getValue().toString());
            }
        }
        return value;
    }

    private Object extractBodyParam(Map<String, ParamInfo> bodyParams) {
        return bodyParams.isEmpty() ? null : bodyParams.values().iterator().next().getValue();
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode URL parameter: " + value, e);
        }
    }
}
