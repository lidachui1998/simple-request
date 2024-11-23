package com.lidachui.simpleRequest.filter;

import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * DefaultRequestFilter
 *
 * @author: lihuijie
 * @date: 2024/11/24 0:51
 * @version: 1.0
 */
@Slf4j
public class DefaultRequestFilter extends AbstractRequestFilter {
    /**
     * 请求前拦截
     *
     * @param request 请求
     */
    @Override
    public void preHandle(Request request) {
        log.info("RequestFilter preHandle");
    }

    /**
     * 请求后拦截
     *
     * @param request 请求
     * @param response 响应
     */
    @Override
    public void afterCompletion(Request request, Response response) {
        log.info("RequestFilter afterCompletion");
    }
}
