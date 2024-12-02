package com.lidachui.simpleRequest.resolver;

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
     * @param params 参数
     * @return {@code Request }
     */
    Request buildRequest(Method method, Object[] args, Object ... params);
}
