package com.lidachui.simpleRequest.resolver;

import com.lidachui.simpleRequest.serialize.Serializer;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Type;

/**
 * AbstractResponseBuilder
 *
 * @author: lihuijie
 * @date: 2024/11/28 0:00
 * @version: 1.0
 */
public abstract class AbstractResponseBuilder implements ResponseBuilder {

    @Getter @Setter private Serializer serializer;

    @Override
    public abstract <T> T buildResponse(Response response, Type responseType);
}
