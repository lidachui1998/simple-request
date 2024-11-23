package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.serialize.DefaultSerializer;
import com.lidachui.simpleRequest.serialize.Serializer;

/**
 * DefaultResponseBuilder
 *
 * @author: lihuijie
 * @date: 2024/11/23 14:08
 * @version: 1.0
 */
public class OkHttpResponseBuilder implements ResponseBuilder{
    private static final Serializer serializer = new DefaultSerializer();

    @Override
    public <T> T buildResponse(Response response, Class<T> responseType) {
        return serializer.deserialize(response.getBody().toString(), responseType);
    }
}