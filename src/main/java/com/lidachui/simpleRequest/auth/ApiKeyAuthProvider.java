package com.lidachui.simpleRequest.auth;

import com.lidachui.simpleRequest.resolver.Request;

import java.util.*;

/**
 * ApiKeyAuthProvider
 *
 * @author: lihuijie
 * @date: 2024/11/22 23:46
 * @version: 1.0
 */
public class ApiKeyAuthProvider implements AuthProvider {

    private final String apiKey;
    private final String headerName;

    public ApiKeyAuthProvider(String apiKey, String headerName) {
        this.apiKey = apiKey;
        this.headerName = headerName;
    }

    @Override
    public void apply(Request request) {
        Map<String, String> headers = request.getHeaders();
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(headerName, apiKey);
    }
}
