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

    private SimpleClient(AbstractHttpClientHandler httpClientHandler, Serializer serializer) {
        this.httpClientHandler = httpClientHandler;
        this.serializer = serializer;
    }

    /** 工厂方法 - 自动配置 */
    public static SimpleClient create() {
        AbstractHttpClientHandler handler;
        if (!SpringUtil.isSpringContextActive()) {
            handler = new RestTemplateHandler();
        } else {
            handler = SpringUtil.getBean(AbstractHttpClientHandler.class);
        }
        Serializer serializer = new JacksonSerializer();
        return new SimpleClient(handler, serializer);
    }

    /** 工厂方法 - 自定义配置 */
    public static SimpleClient create(AbstractHttpClientHandler handler, Serializer serializer) {
        return new SimpleClient(handler, serializer);
    }

    /**
     * 执行请求并返回响应
     *
     * @param request 请求对象
     * @param responseType 响应类型
     * @return Response
     */
    public <T> T execute(Request request, Class<T> responseType) {
        return execute(request, TypeBuilder.type(responseType));
    }

    public <T> T execute(Request request, Type responseType) {
        if (httpClientHandler == null || serializer == null) {
            throw new IllegalStateException("HttpClientHandler and Serializer must not be null");
        }
        request.setSerializer(serializer);
        Response response = httpClientHandler.sendRequest(request);
        DefaultResponseBuilder responseBuilder = new DefaultResponseBuilder();
        responseBuilder.setSerializer(serializer);
        return (T) responseBuilder.buildResponse(response, responseType);
    }
}
