package com.lidachui.simpleRequest.registry;

import com.lidachui.simpleRequest.handler.AbstractHttpClientHandler;
import com.lidachui.simpleRequest.handler.RestTemplateHandler;
import com.lidachui.simpleRequest.resolver.DefaultResponseBuilder;
import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.Response;
import com.lidachui.simpleRequest.serialize.JacksonSerializer;
import com.lidachui.simpleRequest.serialize.Serializer;
import com.lidachui.simpleRequest.util.SpringUtil;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Type;

/**
 * Client
 *
 * @author: lihuijie
 * @date: 2024/11/29 23:51
 * @version: 1.0
 */
@Setter
@Getter
public class SimpleClient {

    private AbstractHttpClientHandler httpClientHandler;

    private Serializer serializer;

    /**
     * 执行
     *
     * @param request 请求
     * @return Response
     */
    public Response execute(Request request, Type responseType) {
        if (httpClientHandler == null) {
            if (!SpringUtil.isSpringContextActive()) {
                httpClientHandler = new RestTemplateHandler();
            } else {
                httpClientHandler = SpringUtil.getBean(AbstractHttpClientHandler.class);
            }
        }
        if (serializer == null) {
            serializer = new JacksonSerializer();
        }
        request.setSerializer(serializer);
        Response response = httpClientHandler.sendRequest(request);
        DefaultResponseBuilder defaultResponseBuilder = new DefaultResponseBuilder();
        defaultResponseBuilder.setSerializer(serializer);
        Object buildResponse = defaultResponseBuilder.buildResponse(response, responseType);
        response.setBody(buildResponse);
        return response;
    }
}
