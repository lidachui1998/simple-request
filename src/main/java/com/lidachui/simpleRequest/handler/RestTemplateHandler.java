package com.lidachui.simpleRequest.handler;

import java.util.Map;

import com.lidachui.simpleRequest.resolver.Request;
import javax.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateHandler
 *
 * @author: lihuijie
 * @date: 2024/11/19 15:45
 * @version: 1.0
 */
public class RestTemplateHandler extends AbstractHttpClientHandler {

    @Resource private ApplicationContext applicationContext;

    @Override
    public <T> T executeRequest(Request request) {
        RestTemplate restTemplate = applicationContext.getBean(RestTemplate.class);
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        Map<String, String> headers = request.getHeaders();
        headers.forEach(httpHeaders::set); // 添加 Headers
        Object body = request.getBody();
        HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);
        ResponseEntity<T> response =
                restTemplate.exchange(
                        request.getUrl(), request.getMethod(), entity, request.getResponseType());
        return response.getBody();
    }

    @Override
    void logRequest(String url, HttpMethod method, Object body, Map<String, String> headers) {
        System.out.println("RestTemplateHandler logRequest");
        super.logRequest(url, method, body, headers);
    }
}
