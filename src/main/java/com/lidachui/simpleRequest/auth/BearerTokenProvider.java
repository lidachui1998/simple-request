package com.lidachui.simpleRequest.auth;

import com.lidachui.simpleRequest.resolver.Request;

import java.util.*;

/**
 * BearerTokenProvider
 *
 * @author: lihuijie
 * @date: 2024/11/22 23:45
 * @version: 1.0
 */
public class BearerTokenProvider implements AuthProvider {

    private final String token;

    public BearerTokenProvider(String token) {
        this.token = token;
    }

    @Override
    public void apply(Request request) {
        Map<String, String> headers = request.getHeaders();
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Authorization", "Bearer " + token);
    }
}
