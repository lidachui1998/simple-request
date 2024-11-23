package com.lidachui.simpleRequest.handler;

import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.Response;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import javax.annotation.Resource;

/**
 * RestTemplateHandler
 *
 * @author: lihuijie
 * @date: 2024/11/19 15:45
 * @version: 1.0
 */
@Slf4j
public class RestTemplateHandler extends AbstractHttpClientHandler {

    @Resource private ApplicationContext applicationContext;

    @Override
    public Response executeRequest(Request request) {
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        Map<String, String> headers = request.getHeaders();
        headers.forEach(httpHeaders::set); // 添加 Headers
        Object body = request.getBody();
        HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);
        ResponseEntity response =
                restTemplate.exchange(
                        request.getUrl(), request.getMethod(), entity, request.getResponseType());
        Map<String, String> headersMap = new HashMap<>();
        response.getHeaders().forEach((k, v) -> headersMap.put(k, v.toString()));
        return new Response(response.getBody(), headersMap);
    }

    public RestTemplate getRestTemplate() {
        try {
            return applicationContext.getBean(RestTemplate.class);
        } catch (NoSuchBeanDefinitionException e) {
            log.error("Error occurred while retrieving RestTemplate bean: " + e.getMessage(), e);
            return new RestTemplate();
        }
    }
}
