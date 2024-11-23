package com.lidachui.simpleRequest.handler;


import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.Response;

/**
 * HttpClientHandler
 *
 * @author: lihuijie
 * @date: 2024/11/19 15:40
 * @version: 1.0
 */
public interface HttpClientHandler {

    /**
     * 发送请求
     *
     * @param request 请求
     * @return {@code T }
     */
    Response sendRequest(
            Request request);
}
