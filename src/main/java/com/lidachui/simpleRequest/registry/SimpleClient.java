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

    /**
     * 工厂方法 - 自动配置
     *
     * 如果当前环境是 Spring，会自动使用 Spring 中的 Bean，否则使用默认的实现。
     */
    public static SimpleClient create() {
        AbstractHttpClientHandler handler;
        if (!SpringUtil.isSpringContextActive()) {
            handler = new RestTemplateHandler(); // 非 Spring 环境使用默认处理器
        } else {
            handler = SpringUtil.getBean(AbstractHttpClientHandler.class); // Spring 环境使用 Spring 管理的 Bean
        }
        Serializer serializer = new JacksonSerializer(); // 默认使用 Jackson 序列化
        return new SimpleClient(handler, serializer);
    }

    /**
     * 工厂方法 - 自定义配置
     *
     * 允许用户自定义 HttpClientHandler 和 Serializer。
     */
    public static SimpleClient create(AbstractHttpClientHandler handler, Serializer serializer) {
        return new SimpleClient(handler, serializer);
    }

    /**
     * 执行
     *
     * @param request 请求
     * @return Response
     */
    public Response execute(Request request, Type responseType) {
        request.setSerializer(serializer);
        Response response = httpClientHandler.sendRequest(request, null);
        DefaultResponseBuilder defaultResponseBuilder = new DefaultResponseBuilder();
        defaultResponseBuilder.setSerializer(serializer);
        Object buildResponse = defaultResponseBuilder.buildResponse(response, responseType);
        response.setBody(buildResponse);
        return response;
    }
}
