package com.lidachui.simpleRequest.resolver;

import lombok.Data;

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
}
