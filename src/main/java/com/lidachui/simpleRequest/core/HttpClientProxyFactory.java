package com.lidachui.simpleRequest.core;

import com.lidachui.simpleRequest.annotation.*;
import com.lidachui.simpleRequest.async.ResponseCallback;
import com.lidachui.simpleRequest.auth.AuthProvider;
import com.lidachui.simpleRequest.constants.BackoffStrategy;
import com.lidachui.simpleRequest.handler.AbstractHttpClientHandler;
import com.lidachui.simpleRequest.handler.HttpClientHandler;
import com.lidachui.simpleRequest.resolver.*;
import com.lidachui.simpleRequest.serialize.Serializer;
import com.lidachui.simpleRequest.util.AnnotationParamExtractor;
import com.lidachui.simpleRequest.util.ParamInfo;
import com.lidachui.simpleRequest.util.SpringUtil;
import com.lidachui.simpleRequest.validator.ResponseValidator;
import com.lidachui.simpleRequest.validator.ValidationResult;

import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.springframework.util.StringUtils;

/**
 * HttpClientProxyFactory
 *
 * @author: lihuijie
 * @date: 2024/12/2 22:49
 * @version: 1.0
 */
@Slf4j
public class HttpClientProxyFactory extends AbstractClientProxyFactory {

    private final RequestBuilder requestBuilder = new HttpRequestBuilder();

    /**
     * 创建代理对象
     *
     * @param clientInterface 客户端接口类
     * @return 客户端代理对象
     */
    @Override
    public <T> T create(Class<T> clientInterface) {
        RestClient restClient = validateRestClientAnnotation(clientInterface);
        String baseUrl = getBaseUrl(restClient.propertyKey(), restClient.baseUrl());
        ResponseValidator responseValidator = getResponseValidator(restClient);
        Serializer serializer = getSerializer(restClient);

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clientInterface);
        enhancer.setCallback(createMethodInterceptor(clientInterface, baseUrl, responseValidator, serializer));

        return (T) enhancer.create();
    }

    /**
     * 验证客户端接口是否有RestClient注解
     *
     * @param clientInterface 客户端接口类
     * @return RestClient注解
     * @throws IllegalArgumentException 如果没有RestClient注解
     */
    private <T> RestClient validateRestClientAnnotation(Class<T> clientInterface) {
        RestClient restClient = clientInterface.getAnnotation(RestClient.class);
        if (restClient == null) {
            throw new IllegalArgumentException(clientInterface.getName() + " is not annotated with @RestClient");
        }
        return restClient;
    }

    /**
     * 创建方法拦截器
     *
     * @param clientInterface 客户端接口类
     * @param baseUrl 基础URL
     * @param responseValidator 响应验证器
     * @param serializer 序列化器
     * @return 方法拦截器
     */
    private MethodInterceptor createMethodInterceptor(Class<?> clientInterface, String baseUrl, ResponseValidator responseValidator, Serializer serializer) {
        return (obj, method, args, proxy) -> {
            RestRequest restRequest = method.getAnnotation(RestRequest.class);
            if (restRequest != null) {
                return handleRestRequest(clientInterface, method, args, baseUrl, responseValidator, serializer);
            }
            return proxy.invokeSuper(obj, args);
        };
    }

    /**
     * 处理Rest请求
     *
     * @param clientInterface 客户端接口类
     * @param method 方法
     * @param args 参数
     * @param baseUrl 基础URL
     * @param responseValidator 响应验证器
     * @param serializer 序列化器
     * @return 请求结果
     */
    private Object handleRestRequest(Class<?> clientInterface, Method method, Object[] args, String baseUrl, ResponseValidator responseValidator, Serializer serializer) {
        Request request = requestBuilder.buildRequest(method, args, baseUrl);
        addAuth(clientInterface, method, request);
        request.setSerializer(serializer);

        HttpClientHandler httpClientHandler = getHttpClientHandler(clientInterface);
        Retry retry = method.getAnnotation(Retry.class);

        if (retry != null) {
            return retryRequest(httpClientHandler, request, responseValidator, retry, method, args);
        }
        return sendRequest(httpClientHandler, request, responseValidator, method, args);
    }

    /**
     * 获取HttpClientHandler
     *
     * @param clientInterface 客户端接口类
     * @return HttpClientHandler实例
     */
    private HttpClientHandler getHttpClientHandler(Class<?> clientInterface) {
        String beanName = clientInterface.getAnnotation(RestClient.class).clientType().getBeanName();
        return getBeanOrCreate(HttpClientHandler.class, beanName);
    }

    /**
     * 判断是否支持指定客户端接口
     *
     * @param clientInterface 客户端接口类
     * @return 是否支持
     */
    @Override
    public boolean supports(Class<?> clientInterface) {
        return clientInterface.isAnnotationPresent(RestClient.class);
    }

    /**
     * 处理重试请求
     *
     * @param httpClientHandler HttpClientHandler实例
     * @param request 请求对象
     * @param responseValidator 响应验证器
     * @param retry 重试注解
     * @param method 方法
     * @param args 参数
     * @return 请求结果
     */
    private Object retryRequest(HttpClientHandler httpClientHandler, Request request, ResponseValidator responseValidator, Retry retry, Method method, Object[] args) {
        int attempts = 0;
        long delay = retry.delay();
        Class<? extends Throwable>[] retryFor = retry.retryFor();
        BackoffStrategy backoffStrategy = retry.backoff();

        while (attempts < retry.maxRetries()) {
            try {
                return sendRequest(httpClientHandler, request, responseValidator, method, args);
            } catch (Throwable e) {
                if (shouldRetry(e, retryFor)) {
                    attempts++;
                    if (attempts >= retry.maxRetries()) {
                        throw e;
                    }
                    delay = adjustDelay(backoffStrategy, delay);
                    sleepBeforeRetry(delay);
                } else {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Max retries reached for request.");
    }

    /**
     * 调整重试延迟
     *
     * @param backoffStrategy 退避策略
     * @param delay 当前延迟
     * @return 调整后的延迟
     */
    private long adjustDelay(BackoffStrategy backoffStrategy, long delay) {
        if (backoffStrategy == BackoffStrategy.EXPONENTIAL) {
            delay *= 2;
        }
        return delay;
    }

    /**
     * 在重试前休眠
     *
     * @param delay 延迟时间
     */
    private void sleepBeforeRetry(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interruptedException);
        }
    }

    /**
     * 发送请求
     *
     * @param httpClientHandler HttpClientHandler实例
     * @param request 请求对象
     * @param responseValidator 响应验证器
     * @param method 方法
     * @param args 参数
     * @return 请求结果
     */
    private Object sendRequest(HttpClientHandler httpClientHandler, Request request, ResponseValidator responseValidator, Method method, Object[] args) {
        AbstractResponseBuilder responseBuilder = getResponseBuilder((AbstractHttpClientHandler) httpClientHandler, request);
        if (method.isAnnotationPresent(Async.class)) {
            return handleAsyncRequest(httpClientHandler, request, responseValidator, method, args, responseBuilder);
        }
        return handleSyncRequest(httpClientHandler, request, responseValidator, method, args, responseBuilder);
    }

    /**
     * 处理异步请求
     *
     * @param httpClientHandler HttpClientHandler实例
     * @param request 请求对象
     * @param responseValidator 响应验证器
     * @param method 方法
     * @param args 参数
     * @param responseBuilder 响应构建器
     * @return null
     */
    private Object handleAsyncRequest(HttpClientHandler httpClientHandler, Request request, ResponseValidator responseValidator, Method method, Object[] args, AbstractResponseBuilder responseBuilder) {
        checkReturnTypeAndParameters(method, args);
        Type callbackType = getCallbackGenericType(method);
        ResponseCallback callback = findCallbackParameter(method, args);

        Retry retry = method.getAnnotation(Retry.class);
        int maxRetries = retry != null ? retry.maxRetries() : 0;
        long delay = retry != null ? retry.delay() : 1000;
        Class<? extends Throwable>[] retryFor = retry != null ? retry.retryFor() : new Class[]{Exception.class};
        BackoffStrategy backoffStrategy = retry != null ? retry.backoff() : BackoffStrategy.FIXED;

        sendRequestWithRetryAsync(httpClientHandler, request, responseValidator, callback, responseBuilder, callbackType, maxRetries, delay, retryFor, backoffStrategy);
        return null;
    }

    /**
     * 处理同步请求
     *
     * @param httpClientHandler HttpClientHandler实例
     * @param request 请求对象
     * @param responseValidator 响应验证器
     * @param method 方法
     * @param args 参数
     * @param responseBuilder 响应构建器
     * @return 请求结果
     */
    private Object handleSyncRequest(HttpClientHandler httpClientHandler, Request request, ResponseValidator responseValidator, Method method, Object[] args, AbstractResponseBuilder responseBuilder) {
        Response response = httpClientHandler.sendRequest(request);
        returnHeaders(method, args, response);
        Object result = responseBuilder.buildResponse(response, method.getGenericReturnType());
        response.setBody(result);
        validateResponse(responseValidator, request, response);
        return result;
    }

    /**
     * 获取响应构建器
     *
     * @param httpClientHandler HttpClientHandler实例
     * @param request 请求对象
     * @return 响应构建器
     */
    @NotNull
    private static AbstractResponseBuilder getResponseBuilder(AbstractHttpClientHandler httpClientHandler, Request request) {
        AbstractResponseBuilder responseBuilder = httpClientHandler.getResponseBuilder();
        responseBuilder.setSerializer(request.getSerializer());
        return responseBuilder;
    }

    /**
     * 返回响应头
     *
     * @param method 方法
     * @param args 参数
     * @param response 响应对象
     */
    private static void returnHeaders(Method method, Object[] args, Response response) {
        Map<String, String> headers = response.getHeaders();
        Map<String, ParamInfo> responseHeaderParams = AnnotationParamExtractor.extractParamsWithType(method, args, ResponseHeader.class, annotation -> ((ResponseHeader) annotation).name());

        if (!responseHeaderParams.isEmpty() && !headers.isEmpty()) {
            injectHeadersIntoParameters(method, args, headers, responseHeaderParams);
        }
    }

    /**
     * 将响应头注入到参数中
     *
     * @param method 方法
     * @param args 参数
     * @param headers 响应头
     * @param responseHeaderParams 响应头参数
     */
    private static void injectHeadersIntoParameters(Method method, Object[] args, Map<String, String> headers, Map<String, ParamInfo> responseHeaderParams) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof ResponseHeader && args[i] instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) args[i];
                    String headerName = ((ResponseHeader) annotation).name();
                    if (headerName == null || headerName.trim().isEmpty()) {
                        map.putAll(headers);
                    } else if (headers.containsKey(headerName)) {
                        map.put(headerName, headers.get(headerName));
                    }
                }
            }
        }
    }

    /**
     * 判断是否应该重试
     *
     * @param throwable 异常
     * @param retryFor 重试的异常类型
     * @return 是否应该重试
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
     * 添加认证信息到请求中
     *
     * @param clientInterface 客户端接口类
     * @param method 方法
     * @param request 请求对象
     */
    private <T> void addAuth(Class<T> clientInterface, Method method, Request request) {
        Auth auth = method.getAnnotation(Auth.class);
        if (auth == null) {
            auth = clientInterface.getAnnotation(Auth.class);
        }
        if (auth != null) {
            AuthProvider authProvider = getBeanOrCreate(auth.provider(), null);
            authProvider.apply(request);
        }
    }

    /**
     * 获取或创建Bean实例
     *
     * @param beanClass Bean类
     * @param beanName Bean名称
     * @return Bean实例
     */
    private <T> T getBeanOrCreate(Class<T> beanClass, String beanName) {
        if (!SpringUtil.isSpringContextActive()) {
            return createInstance(beanClass);
        }
        try {
            if (StringUtils.hasLength(beanName)) {
                return getApplicationContext().getBean(beanName, beanClass);
            } else {
                return getApplicationContext().getBean(beanClass);
            }
        } catch (NoSuchBeanDefinitionException e) {
            log.error("No bean of type " + beanClass.getName() + " found.", e);
            T instance = createInstance(beanClass);
            SpringUtil.registerBean(getApplicationContext(), beanClass);
            return instance;
        }
    }

    /**
     * 创建Bean实例
     *
     * @param beanClass Bean类
     * @return Bean实例
     */
    private <T> T createInstance(Class<T> beanClass) {
        try {
            return beanClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + beanClass.getName(), e);
        }
    }

    /**
     * 获取响应验证器
     *
     * @param restClient RestClient注解
     * @return 响应验证器
     */
    private ResponseValidator getResponseValidator(RestClient restClient) {
        return getBeanOrCreate(restClient.responseValidator(), null);
    }

    /**
     * 获取序列化器
     *
     * @param restClient RestClient注解
     * @return 序列化器
     */
    private Serializer getSerializer(RestClient restClient) {
        return getBeanOrCreate(restClient.serializer(), null);
    }

    /**
     * 验证响应
     *
     * @param responseValidator 响应验证器
     * @param request 请求对象
     * @param response 响应对象
     */
    private void validateResponse(ResponseValidator responseValidator, Request request, Response response) {
        ValidationResult validationResult = responseValidator.validate(response);
        if (!validationResult.isValid()) {
            responseValidator.onFailure(request, response, validationResult);
        }
    }

    /**
     * 检查返回类型和参数
     *
     * @param method 方法
     * @param args 参数
     */
    private void checkReturnTypeAndParameters(Method method, Object[] args) {
        if (method.getReturnType() != void.class) {
            throw new IllegalStateException("Async method " + method.getName() + " must return void");
        }
        if (findCallbackParameter(method, args) == null) {
            throw new IllegalStateException("Async method " + method.getName() + " must have a ResponseCallback parameter annotated with @Callback");
        }
    }

    /**
     * 查找回调参数
     *
     * @param method 方法
     * @param args 参数
     * @return 回调参数
     */
    private ResponseCallback findCallbackParameter(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(Callback.class)) {
                return (ResponseCallback) args[i];
            }
        }
        return null;
    }

    /**
     * 获取回调的泛型类型
     *
     * @param method 方法
     * @return 泛型类型
     */
    private Type getCallbackGenericType(Method method) {
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(Callback.class)) {
                Type type = parameter.getParameterizedType();
                if (type instanceof ParameterizedType) {
                    return ((ParameterizedType) type).getActualTypeArguments()[0];
                }
            }
        }
        throw new IllegalStateException("No generic type found for ResponseCallback");
    }

    /**
     * 异步发送请求并重试
     *
     * @param httpClientHandler HttpClientHandler实例
     * @param request 请求对象
     * @param responseValidator 响应验证器
     * @param callback 回调
     * @param responseBuilder 响应构建器
     * @param callbackType 回调类型
     * @param maxRetries 最大重试次数
     * @param delay 延迟
     * @param retryFor 重试的异常类型
     * @param backoffStrategy 退避策略
     */
    private void sendRequestWithRetryAsync(HttpClientHandler httpClientHandler, Request request, ResponseValidator responseValidator, ResponseCallback callback, AbstractResponseBuilder responseBuilder, Type callbackType, int maxRetries, long delay, Class<? extends Throwable>[] retryFor, BackoffStrategy backoffStrategy) {
        CompletableFuture<Response> future = httpClientHandler.sendRequestAsync(request);

        future.thenAccept(response -> {
            try {
                Object result = responseBuilder.buildResponse(response, callbackType);
                response.setBody(result);
                validateResponse(responseValidator, request, response);
                callback.onSuccess(result);
            } catch (Throwable t) {
                handleFailure(t, callback, maxRetries, delay, retryFor, backoffStrategy, httpClientHandler, request, responseValidator, callbackType, responseBuilder);
            }
        }).exceptionally(throwable -> {
            handleFailure(throwable, callback, maxRetries, delay, retryFor, backoffStrategy, httpClientHandler, request, responseValidator, callbackType, responseBuilder);
            return null;
        });
    }

    /**
     * 处理请求失败
     *
     * @param throwable 异常
     * @param callback 回调
     * @param maxRetries 最大重试次数
     * @param delay 延迟
     * @param retryFor 重试的异常类型
     * @param backoffStrategy 退避策略
     * @param httpClientHandler HttpClientHandler实例
     * @param request 请求对象
     * @param responseValidator 响应验证器
     * @param callbackType 回调类型
     * @param responseBuilder 响应构建器
     */
    private void handleFailure(Throwable throwable, ResponseCallback callback, int maxRetries, long delay, Class<? extends Throwable>[] retryFor, BackoffStrategy backoffStrategy, HttpClientHandler httpClientHandler, Request request, ResponseValidator responseValidator, Type callbackType, AbstractResponseBuilder responseBuilder) {
        if (maxRetries > 0 && shouldRetry(throwable, retryFor)) {
            delay = adjustDelay(backoffStrategy, delay);
            sleepBeforeRetry(delay);
            sendRequestWithRetryAsync(httpClientHandler, request, responseValidator, callback, responseBuilder, callbackType, maxRetries - 1, delay, retryFor, backoffStrategy);
        } else {
            callback.onFailure(throwable);
        }
    }
}
