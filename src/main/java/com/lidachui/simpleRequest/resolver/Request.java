package com.lidachui.simpleRequest.resolver;

import java.util.Map;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpMethod;

/**
 * Request
 *
 * @author: lihuijie
 * @date: 2024/11/21 11:30
 * @version: 1.0
 */
@Data
public class Request<T> {
    /** url */
    private String url;

    /** 方法 */
    private HttpMethod method;

    /** 请求头 */
    private Map<String, String> headers;

    /** 查询参数参数 */
    private Map<String, String> queryParams;

    /** 请求体 */
    private Object body;

    /** 响应类型 */
    private Class<T> responseType;
}
