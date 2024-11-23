package com.lidachui.simpleRequest.filter;

import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.Response;

/**
 * RequestFilter
 *
 * @author: lihuijie
 * @date: 2024/11/24 0:44
 * @version: 1.0
 */
public interface RequestFilter {

    /**
     * 请求前拦截
     *
     * @param request 请求
     */
    void preHandle(Request request);

    /**
     * 请求后拦截
     *
     * @param request 请求
     * @param response 响应
     */
    void afterCompletion(Request request, Response response);

    /**
     * 异常拦截
     *
     * @param request 请求
     * @param response 响应
     * @param e e
     */
    void error(Request request, Response response, Exception e);
}
