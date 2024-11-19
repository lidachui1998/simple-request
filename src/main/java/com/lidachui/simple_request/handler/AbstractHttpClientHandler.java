package com.lidachui.simple_request.handler;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

/**
 * AbstractHttpClientHandler
 *
 * @author: lihuijie
 * @date: 2024/11/19 15:40
 * @version: 1.0
 */
@Slf4j
public abstract class AbstractHttpClientHandler implements HttpClientHandler {

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
    @Override
    public <T> T sendRequest(
            String url,
            HttpMethod method,
            Object body,
            Map<String, String> headers,
            Class<T> responseType) {
        // 请求前日志
        logRequest(url, method, body, headers);
        try {
            T response = executeRequest(url, method, body, headers, responseType);
            // 请求成功后记录响应
            logResponse(url, method, response);
            return response;
        } catch (Exception e) {
            // 请求失败记录异常
            logError(url, method, e);
            throw e; // 重新抛出异常
        }
    }

    // 抽象方法，由子类实现具体的请求逻辑
    protected abstract <T> T executeRequest(
            String url,
            HttpMethod method,
            Object body,
            Map<String, String> headers,
            Class<T> responseType);

    // 请求前日志记录
    void logRequest(String url, HttpMethod method, Object body, Map<String, String> headers) {
        log.info(
                String.format(
                        "HTTP Request:\nURL: %s\nMethod: %s\nBody: %s\nHeaders: %s",
                        url, method, body, headers));
    }

    // 响应日志记录
    void logResponse(String url, HttpMethod method, Object response) {
        log.info(
                String.format(
                        "HTTP Response:\nURL: %s\nMethod: %s\nResponse: %s",
                        url, method, response));
    }

    // 异常日志记录
    void logError(String url, HttpMethod method, Exception e) {
        log.error(
                String.format(
                        "HTTP Error:\nURL: %s\nMethod: %s\nError: %s", url, method, e.getMessage()),
                e);
    }
}
