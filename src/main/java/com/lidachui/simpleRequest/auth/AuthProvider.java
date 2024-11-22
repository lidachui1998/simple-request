package com.lidachui.simpleRequest.auth;

import com.lidachui.simpleRequest.resolver.Request;

/**
 * AuthProvider
 *
 * @author: lihuijie
 * @date: 2024/11/22 23:44
 * @version: 1.0
 */
public interface AuthProvider {

    /**
     * 应用身份验证逻辑到请求中。
     *
     * @param request 请求对象
     */
    void apply(Request request);
}
