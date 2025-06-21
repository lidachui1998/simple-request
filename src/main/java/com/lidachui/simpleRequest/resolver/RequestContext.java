package com.lidachui.simpleRequest.resolver;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * RequestContext
 *
 * @author admin
 * @date 2024/11/24
 */
@Data
public class RequestContext {

    private String requestId;

    private Request request;

    private Response response;

    private Method method;

    private Object[] args;
}
