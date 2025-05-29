package com.lidachui.simpleRequest.resolver;

import lombok.Data;

import java.util.*;

/**
 * Response
 *
 * @author: lihuijie
 * @date: 2024/11/23 11:09
 * @version: 1.0
 */
@Data
public class Response {

    /** 响应体 */
    private Object body;

    /** 头信息 */
    private Map<String, String> headers;

    public Response(Object body, Map<String, String> headers) {
        this.body = body;
        this.headers = headers;
    }

}
