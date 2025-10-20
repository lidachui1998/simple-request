package com.lidachui.simpleRequest.handler;

import com.lidachui.simpleRequest.constants.FilterPhase;
import com.lidachui.simpleRequest.filter.AbstractRequestFilter;
import com.lidachui.simpleRequest.filter.FilterChain;
import com.lidachui.simpleRequest.resolver.AbstractResponseBuilder;
import com.lidachui.simpleRequest.resolver.DefaultResponseBuilder;
import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.RequestContext;
import com.lidachui.simpleRequest.resolver.Response;
import com.lidachui.simpleRequest.util.RequestIdGenerator;
import com.lidachui.simpleRequest.util.SpringUtil;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AbstractHttpClientHandler
 *
 * @author: lihuijie
 * @date: 2024/11/19 15:40
 * @version: 1.0
 */
@Setter
@Getter
@Slf4j
public abstract class AbstractHttpClientHandler implements HttpClientHandler {

    private AbstractResponseBuilder responseBuilder = new DefaultResponseBuilder();

    /**
     * 发送请求
     *
     * @param request 请求
     * @return {@code Response}
     */
    @Override
    public Response sendRequest(Request request, Method method) {
        RequestContext requestContext = createRequestContext(request, method);
        FilterChain filterChain = new FilterChain(getRequestFilters());

        try {
            // 前置处理
            filterChain.doFilter(request, null, requestContext, FilterPhase.PRE_HANDLE);

            // 执行请求
            Response response = executeRequest(request);
            requestContext.setResponse(response);

            // 后置处理
            filterChain.doFilter(request, response, requestContext, FilterPhase.AFTER_COMPLETION);

            return response;
        } catch (Exception e) {
            // 异常处理
            filterChain.doFilter(
                    request, requestContext.getResponse(), requestContext, FilterPhase.ERROR, e);
            throw e;
        }
    }

    /**
     * 异步发送请求
     *
     * @param request 请求
     * @param method
     * @return 可完成未来<response>
     */
    @Override
    public CompletableFuture<Response> sendRequestAsync(Request request, Method method) {
        return CompletableFuture.supplyAsync(() -> sendRequest(request, method));
    }

    // 抽象方法，由子类实现具体的请求逻辑
    protected abstract Response executeRequest(Request request);

    private RequestContext createRequestContext(Request request, Method method) {
        RequestContext requestContext = new RequestContext();
        requestContext.setRequestId(RequestIdGenerator.generate());
        requestContext.setRequest(request);
        if (method != null) {
            requestContext.setMethod(method);
        }
        return requestContext;
    }

    private List<AbstractRequestFilter> getRequestFilters() {
        if (!SpringUtil.isSpringContextActive()) {
            return Collections.emptyList();
        }

        return Optional.of(SpringUtil.getBeansOfType(AbstractRequestFilter.class))
                .map(Map::values)
                .map(
                        filters ->
                                filters.stream()
                                        .sorted(
                                                Comparator.comparing(
                                                        filter ->
                                                                Optional.ofNullable(
                                                                                filter.getClass()
                                                                                        .getAnnotation(
                                                                                                Order
                                                                                                        .class))
                                                                        .map(Order::value)
                                                                        .orElse(Integer.MAX_VALUE)))
                                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
}
