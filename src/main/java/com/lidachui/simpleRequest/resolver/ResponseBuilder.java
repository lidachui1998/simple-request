package com.lidachui.simpleRequest.resolver;

/**
 * ResponseBuilder
 *
 * @author: lihuijie
 * @date: 2024/11/23 11:38
 * @version: 1.0
 */
public interface ResponseBuilder {

    <T> T buildResponse(Response response, Class<T> responseType);
}
