package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.annotation.RestRequest;
import java.lang.reflect.Method;

/**
 * RequestBuilder
 *
 * @author: lihuijie
 * @date: 2024/11/21 11:31
 * @version: 1.0
 */
public interface RequestBuilder {

    /**
     * 构建请求
     *
     * @param method 方法
     * @param args args
     * @param baseUrl 基本url
     * @param restRequest 休息请求
     * @return {@code Request }
     */
    Request buildRequest(Method method, Object[] args, String baseUrl, RestRequest restRequest);
}
