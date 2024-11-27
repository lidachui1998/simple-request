package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.serialize.Serializer;

import java.lang.reflect.Type;

/**
 * DefaultResponseBuilder
 *
 * @author: lihuijie
 * @date: 2024/11/23 14:08
 * @version: 1.0
 */
public class DefaultResponseBuilder extends AbstractResponseBuilder {

    @Override
    public <T> T buildResponse(Response response, Type responseType) {
        Serializer serializer = getSerializer();
        return serializer.deserialize(response.getBody().toString(), responseType);
    }
}
