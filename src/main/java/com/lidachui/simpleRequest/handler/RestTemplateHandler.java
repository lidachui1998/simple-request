package com.lidachui.simpleRequest.handler;

import com.lidachui.simpleRequest.resolver.BinaryAwareResponse;
import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.Response;
import com.lidachui.simpleRequest.util.ContentTypeUtil;
import com.lidachui.simpleRequest.util.SpringUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * RestTemplateHandler
 *
 * @author: lihuijie
 * @date: 2024/11/19 15:45
 * @version: 1.0
 */
@Slf4j
public class RestTemplateHandler extends AbstractHttpClientHandler {

    @Override
    public Response executeRequest(Request request) {
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        Map<String, String> headers = request.getHeaders();
        headers.forEach(httpHeaders::set); // 添加 Headers
        Object body = request.getBody();
        HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);

        // 使用byte[]作为响应类型，以支持二进制数据
        ResponseEntity<byte[]> response =
                restTemplate.exchange(request.getUrl(), request.getMethod(), entity, byte[].class);

        Map<String, String> headersMap = new HashMap<>();
        response.getHeaders().forEach((k, v) -> headersMap.put(k, v.toString()));

        // 获取响应体的字节数组
        byte[] responseBytes = response.getBody() != null ? response.getBody() : new byte[0];

        // 根据Content-Type判断是否为二进制数据
        String contentType = headersMap.getOrDefault("Content-Type", "");
        boolean isBinaryContent = ContentTypeUtil.isBinaryContentType(contentType);

        // 返回支持二进制的响应对象
        return new BinaryAwareResponse(responseBytes, headersMap, isBinaryContent);
    }

    public RestTemplate getRestTemplate() {
        try {
            if (SpringUtil.isSpringContextActive()) {
                return SpringUtil.getBean(RestTemplate.class);
            } else {
                return new RestTemplate();
            }
        } catch (NoSuchBeanDefinitionException e) {
            log.error("Error occurred while retrieving RestTemplate bean: " + e.getMessage(), e);
            return new RestTemplate();
        }
    }
}
