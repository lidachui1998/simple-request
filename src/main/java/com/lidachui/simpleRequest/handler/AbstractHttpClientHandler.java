package com.lidachui.simpleRequest.handler;

import java.util.Map;

import com.lidachui.simpleRequest.resolver.Request;
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
     * @param request 请求
     * @return {@code T }
     */
    @Override
    public <T> T sendRequest(
            Request request) {
        // 请求前日志
        logRequest(request.getUrl(), request.getMethod(), request.getBody(), request.getHeaders());
        try {
            T response = executeRequest(request);
            // 请求成功后记录响应
            logResponse(request.getUrl(), request.getMethod(), response);
            return response;
        } catch (Exception e) {
            // 请求失败记录异常
            logError(request.getUrl(), request.getMethod(), e);
            throw e; // 重新抛出异常
        }
    }

    // 抽象方法，由子类实现具体的请求逻辑
    protected abstract <T> T executeRequest(
           Request request);

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
