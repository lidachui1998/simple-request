package com.lidachui.simpleRequest.resolver;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.lidachui.simpleRequest.serialize.JacksonSerializer;
import com.lidachui.simpleRequest.serialize.Serializer;

import java.lang.reflect.Type;

/**
 * DefaultResponseBuilder
 *
 * @author: lihuijie
 * @date: 2024/11/23 14:08
 * @version: 1.0
 */
public class DefaultResponseBuilder implements ResponseBuilder {
    private static final Serializer serializer = new JacksonSerializer();

    @Override
    public <T> T buildResponse(Response response, Class<T> responseType) {
        JavaType returnType = getReturnType(responseType);
        return serializer.deserialize(response.getBody().toString(), returnType);
    }

    /**
     * 获取返回类型对应的
     *
     * @param returnType 返回类型
     * @return java类型
     */
    private JavaType getReturnType(Type returnType) {
        TypeFactory typeFactory = JacksonSerializer.objectMapper.getTypeFactory();
        return typeFactory.constructType(returnType);
    }
}
