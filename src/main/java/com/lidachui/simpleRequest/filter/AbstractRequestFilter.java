package com.lidachui.simpleRequest.filter;

import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.RequestContext;
import com.lidachui.simpleRequest.resolver.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * AbstractRequestFilter
 *
 * @author: lihuijie
 * @date: 2024/11/24 0:48
 * @version: 1.0
 */
@Slf4j
public abstract class AbstractRequestFilter implements RequestFilter {

    @Getter @Setter private RequestContext requestContext;

    /**
     * 请求前拦截
     *
     * @param request 请求
     */
    @Override
    public abstract void preHandle(Request request);

    /**
     * 请求后拦截
     *
     * @param request 请求
     * @param response 响应
     */
    @Override
    public abstract void afterCompletion(Request request, Response response);

    /**
     * 异常拦截（提供默认实现）
     *
     * @param request 请求
     * @param response 响应
     * @param e 异常
     */
    @Override
    public void error(Request request, Response response, Exception e) {
        // 默认实现：打印异常信息
        log.error("Error occurred while processing the request:", e);
        log.error("Request: " + request);
        log.error("Response: " + response);
        e.printStackTrace();
    }
}
