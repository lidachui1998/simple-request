package com.lidachui.simpleRequest.handler;

import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.Response;
import com.lidachui.simpleRequest.serialize.JacksonSerializer;
import com.lidachui.simpleRequest.serialize.Serializer;

import kotlin.Pair;

import okhttp3.*;

import java.io.IOException;
import java.util.*;

/**
 * OkHttpHandler
 *
 * @author: lihuijie
 * @date: 2024/11/23 10:29
 * @version: 1.0
 */
public class OkHttpHandler extends AbstractHttpClientHandler {

    private final OkHttpClient client;

    private final Serializer serializer = new JacksonSerializer();

    public OkHttpHandler() {
        this.client = new OkHttpClient();
    }

    @Override
    protected Response executeRequest(Request request) {
        // 构建 OkHttp 请求
        okhttp3.Request.Builder requestBuilder =
                new okhttp3.Request.Builder().url(request.getUrl());

        try {
            // 设置请求头
            if (request.getHeaders() != null) {
                Set set = request.getHeaders().entrySet();
                for (Object o : set) {
                    Map.Entry entry = (Map.Entry) o;
                    String key = (String) entry.getKey();
                    String value = (String) entry.getValue();
                    requestBuilder.addHeader(key, value);
                }
            }

            // 构建请求体
            RequestBody requestBody;
            Map<String, String> headers = request.getHeaders();
            // 根据 Content-Type 来决定请求体的构建方式
            if ("application/x-www-form-urlencoded"
                    .equalsIgnoreCase(headers.getOrDefault("Content-Type",""))) {
                requestBody = buildFormRequestBody(request.getBody());
            } else {
                requestBody = buildRequestBody(request.getBody());
            }

            switch (request.getMethod()) {
                case GET:
                    requestBuilder.get();
                    break;
                case POST:
                    requestBuilder.post(requestBody);
                    break;
                case PUT:
                    requestBuilder.put(requestBody);
                    break;
                case DELETE:
                    if (requestBody != null) {
                        requestBuilder.delete(requestBody);
                    } else {
                        requestBuilder.delete();
                    }
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Unsupported HTTP method: " + request.getMethod());
            }

            // 执行请求并处理响应
            okhttp3.Response response = client.newCall(requestBuilder.build()).execute();
            if (response.isSuccessful()) {
                Map<String, String> headersMap = new HashMap<>();
                Headers responseHeaders = response.headers();
                for (Pair<? extends String, ? extends String> header : responseHeaders) {
                    String first = header.getFirst();
                    String second = header.getSecond();
                    headersMap.put(first, second);
                }
                String responseBody = response.body() != null ? response.body().string() : "";
                return new Response(responseBody, headersMap);
            } else {
                throw new IOException("Request failed with status code: " + response.code());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建 JSON 请求体
     *
     * @param body 请求体
     * @return 请求正文
     */
    private RequestBody buildRequestBody(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof String) {
            return RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), (String) body);
        } else {
            return RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), serializer.serialize(body));
        }
    }

    /**
     * 构建表单请求体
     *
     * @param body 请求体
     * @return 请求正文
     */
    private RequestBody buildFormRequestBody(Object body) {
        if (body == null) {
            return null;
        }

        // 假设请求体是一个 Map<String, String>，代表表单字段和值
        if (body instanceof Map) {
            Map<String, String> formFields = (Map<String, String>) body;
            FormBody.Builder formBuilder = new FormBody.Builder();
            for (Map.Entry<String, String> entry : formFields.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
            return formBuilder.build();
        }

        // 如果不是 Map，则抛出异常或进行其他处理
        throw new IllegalArgumentException("Body must be a Map<String, String> for form data.");
    }
}
