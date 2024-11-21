package com.lidachui.simpleRequest.core;

import com.lidachui.simpleRequest.annotation.RestClient;
import com.lidachui.simpleRequest.annotation.RestRequest;
import com.lidachui.simpleRequest.resolver.DefaultRequestBuilder;
import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.RequestBuilder;
import com.lidachui.simpleRequest.validator.ResponseValidator;
import com.lidachui.simpleRequest.validator.ValidationResult;
import com.lidachui.simpleRequest.handler.HttpClientHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.context.ApplicationContext;

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
                                String beanName = restClient.clientType().getBeanName();
                                HttpClientHandler httpClientHandler =
                                        applicationContext.getBean(
                                                beanName, HttpClientHandler.class);

                                // 发送请求并获取响应
                                Object response =
                                        httpClientHandler.sendRequest(
                                                request.getUrl(),
                                                request.getMethod(),
                                                request.getBody(),
                                                request.getHeaders(),
                                                method.getReturnType());

                                // 校验响应
                                validateResponse(responseValidator, request, response);
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
     * @param request 请求信息
     */
    private void validateResponse(
            ResponseValidator responseValidator, Request request, Object response) {
        ValidationResult validationResult = responseValidator.validate(response);
        if (!validationResult.isValid()) {
            // 调用用户自定义的校验失败处理方法
            responseValidator.onFailure(request, response, validationResult);
        }
    }
}
