package com.lidachui.simpleRequest.resolver;


/**
 * RestTemplateResponseBuilder
 *
 * @author: lihuijie
 * @date: 2024/11/23 14:12
 * @version: 1.0
 */
public class RestTemplateResponseBuilder implements ResponseBuilder {
    @Override
    public <T> T buildResponse(Response response, Class<T> responseType) {
        return (T) response.getBody();
    }
}
