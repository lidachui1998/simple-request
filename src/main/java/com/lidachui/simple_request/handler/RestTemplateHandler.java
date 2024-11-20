package com.lidachui.simple_request.handler;

import java.util.Map;
import javax.annotation.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateHandler
 *
 * @author: lihuijie
 * @date: 2024/11/19 15:45
 * @version: 1.0
 */
public class RestTemplateHandler extends AbstractHttpClientHandler {

  @Resource
  private RestTemplate restTemplate;

  @Override
  public <T> T executeRequest(String url, HttpMethod method, Object body,
      Map<String, String> headers, Class<T> responseType) {
    if (restTemplate == null){
      restTemplate = new RestTemplate();
    }
    HttpHeaders httpHeaders = new HttpHeaders();
    headers.forEach(httpHeaders::set); // 添加 Headers

    HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);
    ResponseEntity<T> response = restTemplate.exchange(url, method, entity, responseType);
    return response.getBody();
  }

  @Override
  void logRequest(String url, HttpMethod method, Object body, Map<String, String> headers) {
        System.out.println("RestTemplateHandler logRequest");
    super.logRequest(url, method, body, headers);
  }
}
