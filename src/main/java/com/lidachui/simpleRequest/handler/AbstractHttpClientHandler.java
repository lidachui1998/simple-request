package com.lidachui.simpleRequest.handler;

import com.lidachui.simpleRequest.filter.AbstractRequestFilter;
import com.lidachui.simpleRequest.resolver.*;
import com.lidachui.simpleRequest.util.RequestIdGenerator;
import com.lidachui.simpleRequest.util.SpringUtil;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.CollectionUtils;

import java.util.*;

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

    private ResponseBuilder responseBuilder = new DefaultResponseBuilder();

    /**
     * 发送请求
     *
     * @param request 请求
     * @return {@code Response}
     */
    @Override
    public Response sendRequest(Request request) {
        RequestContext requestContext = new RequestContext();
        // 记录请求上下文
        requestContext.setRequestId(RequestIdGenerator.generate());
        requestContext.setRequest(request);
        // 调用所有 RequestFilter 的 preHandle
        invokePreHandleFilters(request, requestContext);
        Response response = null;
        try {
            response = executeRequest(request);
            requestContext.setResponse(response);
            // 调用所有 RequestFilter 的 afterCompletion
            invokeAfterCompletionFilters(request, response, requestContext);
            return response;
        } catch (Exception e) {
            // 调用所有 RequestFilter 的 error
            invokeErrorFilters(request, response, e, requestContext);
            throw e; // 重新抛出异常
        }
    }

    // 抽象方法，由子类实现具体的请求逻辑
    protected abstract Response executeRequest(Request request);

    // 调用所有 preHandle
    private void invokePreHandleFilters(Request request, RequestContext requestContext) {
        List<AbstractRequestFilter> requestFilters = getRequestFilters();
        for (AbstractRequestFilter filter : requestFilters) {
            try {
                filter.setRequestContext(requestContext);
                filter.preHandle(request);
            } catch (Exception e) {
                log.warn("Error executing preHandle filter: {}", filter.getClass().getName(), e);
            }
        }
    }

    // 调用所有 afterCompletion
    private void invokeAfterCompletionFilters(
            Request request, Response response, RequestContext requestContext) {
        List<AbstractRequestFilter> requestFilters = getRequestFilters();
        for (AbstractRequestFilter filter : requestFilters) {
            try {
                filter.setRequestContext(requestContext);
                filter.afterCompletion(request, response);
            } catch (Exception e) {
                log.warn(
                        "Error executing afterCompletion filter: {}",
                        filter.getClass().getName(),
                        e);
            }
        }
    }

    // 调用所有 error
    private void invokeErrorFilters(
            Request request, Response response, Exception e, RequestContext requestContext) {
        List<AbstractRequestFilter> requestFilters = getRequestFilters();
        for (AbstractRequestFilter filter : requestFilters) {
            try {
                filter.setRequestContext(requestContext);
                filter.error(request, response, e);
            } catch (Exception ex) {
                log.warn("Error executing error filter: {}", filter.getClass().getName(), ex);
            }
        }
    }

    private List<AbstractRequestFilter> getRequestFilters() {
        List<AbstractRequestFilter> requestFilters = new ArrayList<>();
        Map<String, AbstractRequestFilter> beans =
                SpringUtil.getBeansOfType(AbstractRequestFilter.class);
        Collection<AbstractRequestFilter> values = beans.values();
        if (!CollectionUtils.isEmpty(values)) {
            requestFilters.addAll(values);
        }
        return requestFilters;
    }
}
