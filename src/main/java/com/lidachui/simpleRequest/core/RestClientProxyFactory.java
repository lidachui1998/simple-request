package com.lidachui.simpleRequest.core;

import com.lidachui.simpleRequest.annotation.BodyParam;
import com.lidachui.simpleRequest.annotation.HeaderParam;
import com.lidachui.simpleRequest.annotation.QueryParam;
import com.lidachui.simpleRequest.annotation.RestClient;
import com.lidachui.simpleRequest.annotation.RestRequest;
import com.lidachui.simpleRequest.handler.HttpClientHandler;
import com.lidachui.simpleRequest.validator.ResponseValidator;
import com.lidachui.simpleRequest.validator.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * RestClientProxyFactory
 *
 * @author: lihuijie
 * @date: 2024/11/18 21:50
 * @version: 1.0
 */
@Slf4j
public class RestClientProxyFactory {

    private ApplicationContext applicationContext;

    /** 创建指定接口的 REST 客户端代理。 */
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clientInterface) {
        RestClient restClient = clientInterface.getAnnotation(RestClient.class);
        if (restClient == null) {
            throw new IllegalArgumentException(
                    clientInterface.getName() + " is not annotated with @RestClient");
        }

        String baseUrl = getBaseUrl(restClient);

        // 获取自定义的校验器
        ResponseValidator responseValidator = getResponseValidator(restClient);

        // 使用 CGLIB 创建代理对象
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clientInterface);
        enhancer.setCallback(
                (MethodInterceptor)
                        (obj, method, args, proxy) -> {
                            RestRequest restRequest = method.getAnnotation(RestRequest.class);
                            if (restRequest != null) {
                                String url =
                                        buildFullUrl(baseUrl, restRequest.path(), method, args);
                                Map<String, String> headerMap =
                                        parseHeaders(restRequest.headers(), method, args);
                                Map<String, String> queryMap =
                                        buildQueryMap(restRequest.queryParams(), method, args);

                                String fullUrl = buildUrlWithQueryParams(url, queryMap);
                                HttpMethod httpMethod = restRequest.method();
                                Object body = extractBodyParam(method, args);
                                String beanName = restClient.clientType().getBeanName();
                                HttpClientHandler httpClientHandler =
                                        applicationContext.getBean(
                                                beanName, HttpClientHandler.class);

                                // 发送请求并获取响应
                                Object response =
                                        httpClientHandler.sendRequest(
                                                fullUrl,
                                                httpMethod,
                                                body,
                                                headerMap,
                                                method.getReturnType());

                                // 校验响应
                                validateResponse(responseValidator, response);
                                return response;
                            }
                            return proxy.invokeSuper(obj, args);
                        });

        // 创建代理对象
        return (T) enhancer.create();
    }

    /**
     * 获取基本url
     *
     * @param restClient 休息客户端
     * @return {@code String }
     */
    private String getBaseUrl(RestClient restClient) {
        String propertyKey = restClient.propertyKey();
        if (propertyKey != null && !propertyKey.isEmpty()) {
            String propertyValue = applicationContext.getEnvironment().getProperty(propertyKey);
            return (propertyValue != null && !propertyValue.isEmpty())
                    ? propertyValue
                    : restClient.baseUrl();
        }
        return restClient.baseUrl();
    }

    /**
     * 获取响应验证器
     *
     * @param restClient 休息客户端
     * @return {@code ResponseValidator }
     */
    private ResponseValidator getResponseValidator(RestClient restClient) {
        return applicationContext.getBean(restClient.responseValidator());
    }

    /**
     * 验证响应
     *
     * @param responseValidator 响应验证器
     * @param response 响应
     */
    private void validateResponse(ResponseValidator responseValidator, Object response) {
        ValidationResult validationResult = responseValidator.validate(response);
        if (!validationResult.isValid()) {
            // 调用用户自定义的校验失败处理方法
            responseValidator.onFailure(validationResult.getErrorMessage());
        }
    }

    /**
     * 构建完整url
     *
     * @param baseUrl 基本url
     * @param path 路径
     * @param method 方法
     * @param args args
     * @return {@code String }
     */
    private String buildFullUrl(String baseUrl, String path, Method method, Object[] args) {
        return baseUrl + replacePathVariables(path, method, args);
    }

    /**
     * 建造query参数
     *
     * @param queryParams 查询参数参数
     * @param method 方法
     * @param args args
     * @return {@code Map<String, String> }
     */
    private Map<String, String> buildQueryMap(String[] queryParams, Method method, Object[] args) {
        Map<String, String> queryMap = parseKeyValuePairs(queryParams);
        queryMap.putAll(extractMethodParams(method, args));
        return queryMap;
    }

    /**
     * 替换路径变量
     *
     * @param path 路径
     * @param method 方法
     * @param args args
     * @return {@code String }
     */
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

    /**
     * 提取方法参数
     *
     * @param method 方法
     * @param args args
     * @return {@code Map<String, String> }
     */
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

    /**
     * 解析请求头
     *
     * @param headers 请求头
     * @param method 方法
     * @param args args
     * @return {@code Map<String, String> }
     */
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

    /**
     * 解析标头参数值
     *
     * @param value 价值
     * @param method 方法
     * @param args args
     * @return {@code String }
     */
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

    /**
     * 解析键值对
     *
     * @param keyValuePairs 键值对
     * @return {@code Map<String, String> }
     */
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

    /**
     * 建造url具有查询参数参数
     *
     * @param url url
     * @param queryParams 查询参数参数
     * @return {@code String }
     */
    private String buildUrlWithQueryParams(String url, Map<String, String> queryParams) {
        if (queryParams.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        sb.append("?");
        queryParams.forEach((key, value) -> sb.append(key).append("=").append(value).append("&"));
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * 提取body参数
     *
     * @param method 方法
     * @param args args
     * @return {@code Object }
     */
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

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
