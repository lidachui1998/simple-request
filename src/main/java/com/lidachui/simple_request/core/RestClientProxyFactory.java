package com.lidachui.simple_request.core;

import com.lidachui.simple_request.annotation.*;
import com.lidachui.simple_request.constants.RequestClientType;
import com.lidachui.simple_request.handler.HttpClientHandler;
import com.lidachui.simple_request.validator.ResponseValidator;
import com.lidachui.simple_request.validator.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

    /**
     * 创建指定接口的 REST 客户端代理。
     *
     * @param clientInterface 客户端接口类
     * @param <T>             客户端接口类型
     * @return 客户端接口的代理实例
     * @throws IllegalArgumentException 如果接口未被 @RestClient 注解
     */
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clientInterface) {
        RestClient restClient = clientInterface.getAnnotation(RestClient.class);
        if (restClient == null) {
            throw new IllegalArgumentException(
                clientInterface.getName() + " is not annotated with @RestClient");
        }

        String baseUrl;
        String propertyKey = restClient.propertyKey();
        if (propertyKey != null && !propertyKey.isEmpty()) {
            String propertyValue = applicationContext.getEnvironment().getProperty(propertyKey);
            if (propertyValue != null && !propertyValue.isEmpty()) {
                baseUrl = propertyValue;
            } else {
                baseUrl = restClient.baseUrl();
            }
        } else {
            baseUrl = restClient.baseUrl();
        }

        // 获取自定义的校验器
        Class<? extends ResponseValidator> validatorClass = restClient.responseValidator();
        ResponseValidator responseValidator = applicationContext.getBean(validatorClass);

        // 使用 CGLIB 创建代理对象
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clientInterface); // 指定代理的父类
        enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
            RestRequest restRequest = method.getAnnotation(RestRequest.class);
            if (restRequest != null) {
                String url = buildFullUrl(baseUrl, restRequest.path(), method, args);
                Map<String, String> headerMap = parseHeaders(restRequest.headers(), method, args);
                Map<String, String> queryMap = buildQueryMap(restRequest.queryParams(), method, args);

                String fullUrl = buildUrlWithQueryParams(url, queryMap);
                HttpMethod httpMethod = restRequest.method();
                Object body = extractBodyParam(method, args);
                RequestClientType requestClientType = restClient.clientType();
                String beanName = requestClientType.getBeanName();
                HttpClientHandler httpClientHandler = applicationContext.getBean(beanName, HttpClientHandler.class);

                // 发送请求并获取响应
                Object response = httpClientHandler.sendRequest(
                    fullUrl,
                    httpMethod,
                    body,
                    headerMap,
                    method.getReturnType());

                // 校验响应
                ValidationResult validationResult = responseValidator.validate(response);
                if (!validationResult.isValid()) {
                    // 调用用户自定义的校验失败处理方法
                    responseValidator.onFailure(validationResult.getErrorMessage());
                }
                return response;
            }
            return proxy.invokeSuper(obj, args);
        });

        // 创建代理对象
        return (T) enhancer.create();
    }
    /**
     * 构建完整的 URL。
     *
     * @param baseUrl 基础 URL
     * @param path    请求路径
     * @param method  方法对象
     * @param args    方法参数
     * @return 完整的 URL
     */
    private String buildFullUrl(String baseUrl, String path, Method method, Object[] args) {
        return baseUrl + replacePathVariables(path, method, args);
    }

    /**
     * 构建查询参数的 Map。
     *
     * @param queryParams 查询参数数组
     * @param method      方法对象
     * @param args        方法参数
     * @return 查询参数的 Map
     */
    private Map<String, String> buildQueryMap(String[] queryParams, Method method, Object[] args) {
        Map<String, String> queryMap = parseKeyValuePairs(queryParams);
        queryMap.putAll(extractMethodParams(method, args));
        return queryMap;
    }

    /**
     * 替换路径中的变量。
     *
     * @param path   请求路径
     * @param method 方法对象
     * @param args   方法参数
     * @return 替换后的路径
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
     * 提取方法参数中的查询参数。
     *
     * @param method 方法对象
     * @param args   方法参数
     * @return 查询参数的 Map
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
     * 解析请求头。
     *
     * @param headers 请求头数组
     * @param method  方法对象
     * @param args    方法参数
     * @return 请求头的 Map
     */
    private Map<String, String> parseHeaders(String[] headers, Method method, Object[] args) {
        Map<String, String> headerMap = new HashMap<>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (String header : headers) {
            String[] split = header.split(":", 2);
            if (split.length == 2) {
                String key = split[0].trim();
                String value = split[1].trim();

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

                headerMap.put(key, value);
            }
        }

        return headerMap;
    }

    /**
     * 解析键值对。
     *
     * @param keyValuePairs 键值对数组
     * @return 键值对的 Map
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
     * 构建带查询参数的 URL。
     *
     * @param url         基础 URL
     * @param queryParams 查询参数的 Map
     * @return 带查询参数的完整 URL
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
     * 提取方法参数中的请求体。
     *
     * @param method 方法对象
     * @param args   方法参数
     * @return 请求体对象
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
