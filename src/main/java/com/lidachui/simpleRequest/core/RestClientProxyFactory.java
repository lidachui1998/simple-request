package com.lidachui.simpleRequest.core;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.lidachui.simpleRequest.annotation.*;
import com.lidachui.simpleRequest.auth.AuthProvider;
import com.lidachui.simpleRequest.constants.BackoffStrategy;
import com.lidachui.simpleRequest.handler.AbstractHttpClientHandler;
import com.lidachui.simpleRequest.handler.HttpClientHandler;
import com.lidachui.simpleRequest.resolver.*;
import com.lidachui.simpleRequest.util.AnnotationParamExtractor;
import com.lidachui.simpleRequest.util.ParamInfo;
import com.lidachui.simpleRequest.validator.ResponseValidator;
import com.lidachui.simpleRequest.validator.ValidationResult;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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

    @Setter private ApplicationContext applicationContext;

    private final RequestBuilder requestBuilder = new DefaultRequestBuilder(); // 默认实现

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
                                Request request =
                                        requestBuilder.buildRequest(
                                                method, args, baseUrl, restRequest);

                                // 获取验证提供器
                                addAuth(clientInterface, method, request);

                                String beanName = restClient.clientType().getBeanName();
                                HttpClientHandler httpClientHandler =
                                        applicationContext.getBean(
                                                beanName, HttpClientHandler.class);

                                // 检查是否需要重试
                                Retry retry = method.getAnnotation(Retry.class);
                                if (retry != null) {
                                    return retryRequest(
                                            httpClientHandler,
                                            request,
                                            responseValidator,
                                            retry,
                                            method,
                                            args);
                                }
                                // 发送请求并获取响应
                                return sendRequest(
                                        httpClientHandler,
                                        request,
                                        responseValidator,
                                        method,
                                        args);
                            }
                            return proxy.invokeSuper(obj, args);
                        });

        // 创建代理对象
        return (T) enhancer.create();
    }

    /**
     * 重试请求的逻辑
     *
     * @param httpClientHandler http客户端处理程序
     * @param request 请求
     * @param responseValidator 响应验证器
     * @param retry 重试
     * @return 对象
     */
    private Object retryRequest(
            HttpClientHandler httpClientHandler,
            Request request,
            ResponseValidator responseValidator,
            Retry retry,
            Method method,
            Object[] args) {
        int attempts = 0;
        long delay = retry.delay();
        Class<? extends Throwable>[] retryFor = retry.retryFor();
        BackoffStrategy backoffStrategy = retry.backoff();

        while (attempts < retry.maxRetries()) {
            try {
                return sendRequest(httpClientHandler, request, responseValidator, method, args);
            } catch (Throwable e) {
                // 如果异常是指定的重试异常之一，则重试
                if (shouldRetry(e, retryFor)) {
                    attempts++;
                    if (attempts >= retry.maxRetries()) {
                        throw e; // 达到最大重试次数，抛出异常
                    }

                    // 根据退避策略调整延迟
                    if (backoffStrategy == BackoffStrategy.EXPONENTIAL) {
                        delay *= 2; // 指数退避
                    }

                    try {
                        Thread.sleep(delay); // 延迟重试
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(interruptedException);
                    }
                } else {
                    throw e; // 如果异常不在重试列表中，直接抛出
                }
            }
        }

        throw new IllegalStateException("Max retries reached for request.");
    }

    private Object sendRequest(
            HttpClientHandler httpClientHandler,
            Request request,
            ResponseValidator responseValidator,
            Method method,
            Object[] args) {
        // 发送请求并获取响应
        Response response = httpClientHandler.sendRequest(request);

        // 提取响应头并注入到 Map 类型的参数
        returnHeaders(method, args, response);

        // 校验响应
        validateResponse(responseValidator, request, response);

        AbstractHttpClientHandler abstractHttpClientHandler =
                (AbstractHttpClientHandler) httpClientHandler;
        ResponseBuilder responseBuilder = abstractHttpClientHandler.getResponseBuilder();
        return responseBuilder.buildResponse(response, method.getReturnType());
    }

    private static void returnHeaders(Method method, Object[] args, Response response) {
        Map<String, String> headers = response.getHeaders();

        Map<String, ParamInfo> responseHeaderParams =
                AnnotationParamExtractor.extractParamsWithType(
                        method,
                        args,
                        ResponseHeader.class,
                        annotation -> ((ResponseHeader) annotation).name());

        if (!responseHeaderParams.isEmpty() && !headers.isEmpty()) {
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();

            for (int i = 0; i < parameterAnnotations.length; i++) {
                for (Annotation annotation : parameterAnnotations[i]) {
                    if (annotation instanceof ResponseHeader && args[i] instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) args[i];

                        // 获取 ResponseHeader 的 name 值
                        String headerName = ((ResponseHeader) annotation).name();

                        if (headerName == null || headerName.trim().isEmpty()) {
                            // 如果 name 为空，注入所有头部信息
                            map.putAll(headers);
                        } else {
                            // 注入指定的头部信息
                            if (headers.containsKey(headerName)) {
                                map.put(headerName, headers.get(headerName));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 判断是否应该重试
     *
     * @param throwable 可投掷
     * @param retryFor 重试
     * @return boolean
     */
    private boolean shouldRetry(Throwable throwable, Class<? extends Throwable>[] retryFor) {
        for (Class<? extends Throwable> retryException : retryFor) {
            if (retryException.isInstance(throwable)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 添加身份验证
     *
     * @param clientInterface 客户端界面
     * @param method 方法
     * @param request 请求
     */
    private <T> void addAuth(Class<T> clientInterface, Method method, Request request) {
        Auth auth = method.getAnnotation(Auth.class);
        if (auth == null) {
            auth = clientInterface.getAnnotation(Auth.class); // 尝试从接口级别获取
        }
        if (auth != null) {
            AuthProvider authProvider;
            try {
                authProvider = applicationContext.getBean(auth.provider());
            } catch (NoSuchBeanDefinitionException e) {
                log.error(
                        "No bean of type AuthProvider found for auth annotation in class "
                                + clientInterface.getName(),
                        e);
                try {
                    authProvider = auth.provider().getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            authProvider.apply(request);
        }
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
     * @param request 请求信息
     */
    private void validateResponse(
            ResponseValidator responseValidator, Request request, Response response) {
        ValidationResult validationResult = responseValidator.validate(response);
        if (!validationResult.isValid()) {
            // 调用用户自定义的校验失败处理方法
            responseValidator.onFailure(request, response, validationResult);
        }
    }
}
