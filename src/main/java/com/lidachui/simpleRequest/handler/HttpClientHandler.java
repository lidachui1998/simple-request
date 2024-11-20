package com.lidachui.simpleRequest.handler;

import java.util.Map;
import org.springframework.http.HttpMethod;

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
     * @param url url
     * @param method 方法
     * @param body 请求体
     * @param headers 请求头
     * @param responseType 响应类型
     * @return {@code T }
     */
    <T> T sendRequest(
            String url,
            HttpMethod method,
            Object body,
            Map<String, String> headers,
            Class<T> responseType);
}
