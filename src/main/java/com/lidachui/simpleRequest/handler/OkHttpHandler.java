package com.lidachui.simpleRequest.handler;

import com.lidachui.simpleRequest.resolver.ByteResponse;
import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.Response;
import com.lidachui.simpleRequest.serialize.JacksonSerializer;
import com.lidachui.simpleRequest.serialize.Serializer;
import com.lidachui.simpleRequest.util.ContentTypeUtil;

import com.lidachui.simpleRequest.util.ExceptionUtil;
import com.lidachui.simpleRequest.util.ObjectUtil;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import okhttp3.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        okhttp3.Request.Builder requestBuilder =
            new okhttp3.Request.Builder().url(request.getUrl());

        try {
            // 设置请求头
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(requestBuilder::addHeader);
            }

            // 构建请求体
            RequestBody requestBody;
            Map<String, String> headers = request.getHeaders();
            String contentType = headers != null ? headers.getOrDefault("Content-Type", "") : "";
            if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
                requestBody = buildFormRequestBody(request, request.getBody());
            } else {
                requestBody = buildRequestBody(request.getBody());
            }

            // 设置 HTTP 方法
            switch (request.getMethod()) {
                case GET:
                    requestBuilder.get();
                    break;
                case POST:
                    requestBuilder.post(requestBody != null ? requestBody : new FormBody.Builder().build());
                    break;
                case PUT:
                    requestBuilder.put(requestBody != null ? requestBody : new FormBody.Builder().build());
                    break;
                case DELETE:
                    if (requestBody != null) {
                        requestBuilder.delete(requestBody);
                    } else {
                        requestBuilder.delete();
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported HTTP method: " + request.getMethod());
            }

            // 执行请求
            try (okhttp3.Response response = client.newCall(requestBuilder.build()).execute()) {

                Map<String, String> headersMap = new HashMap<>();
                Headers responseHeaders = response.headers();
                for (String name : responseHeaders.names()) {
                    headersMap.put(name, responseHeaders.get(name));
                }

                if (response.isSuccessful()) {
                    byte[] bodyBytes = response.body() != null ? response.body().bytes() : new byte[0];

                    String ct = headersMap.entrySet().stream()
                        .filter(e -> "Content-Type".equalsIgnoreCase(e.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse("");

                    boolean isBinary = ContentTypeUtil.isBinaryContentType(ct);
                    return new ByteResponse(bodyBytes, headersMap, isBinary);
                } else{
                    String responseBody = "";
                    if (response.body() != null) {
                        try (InputStream is = response.body().byteStream()) {
                            responseBody = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
                        }
                    }
                    // 将 okhttp3.Headers -> Map<String, List<String>> -> HttpHeaders
                    HttpHeaders httpHeaders = new HttpHeaders();
                    Map<String, List<String>> multiMap = response.headers().toMultimap();
                    multiMap.forEach((k, v) -> {
                        if (k != null) {
                            httpHeaders.put(k, v); // HttpHeaders 接受 List<String>
                        }
                    });
                    throw new HttpClientErrorException(
                        HttpStatus.valueOf(response.code()),
                        response.message(),
                        httpHeaders,
                        responseBody.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8
                    );
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Network request failed", e);
        } catch (Exception e) {
            ExceptionUtil.rethrow(e);
        }
        return new ByteResponse(new byte[0], null,false);
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
     * @param request 请求
     * @param body 请求体
     * @return 请求正文
     */
    private RequestBody buildFormRequestBody(Request request, Object body) {
        if (body == null) {
            return null;
        }

        Map<String, String> formFields;

        // 如果是 Map 类型，直接转换
        if (body instanceof Map) {
            formFields = (Map<String, String>) body;
        }
        // 如果是 Java Bean，尝试通过反射转换为 Map
        else if (body instanceof Object) {
            formFields = convertObjectToMap(body);
        }
        // 如果是 JSON 字符串，尝试解析为 Map
        else if (body instanceof String) {
            try {
                formFields = parseJsonToMap(request, (String) body);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid JSON string for form data.", e);
            }
        }
        // 不支持其他类型
        else {
            throw new IllegalArgumentException(
                    "Body must be a Map<String, String>, a bean, or a JSON string for form data.");
        }

        // 构建 FormBody
        FormBody.Builder formBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : formFields.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
        }
        return formBuilder.build();
    }

    /**
     * 将 Java Bean 转换为 Map<String, String>
     *
     * @param obj Java Bean 对象
     * @return Map 表单字段和值
     */
    public static Map<String, String> convertObjectToMap(Object obj) {
        Map<String, String> map = new HashMap<>();
        Class<?> current = obj.getClass();

        while (current != null) {
            // 1. 提取字段值
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())
                        || Modifier.isTransient(field.getModifiers())) {
                    continue; // 跳过 static 和 transient 字段
                }
                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    if (value != null) {
                        map.putIfAbsent(field.getName(), value.toString());
                    }
                } catch (IllegalAccessException ignored) {
                    throw new RuntimeException(ignored);
                }
            }

            // 2. 提取 Bean 属性值
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(current);
                for (PropertyDescriptor property : beanInfo.getPropertyDescriptors()) {
                    String name = property.getName();
                    if ("class".equals(name) || map.containsKey(name)) {
                        continue; // 跳过 class 属性或已处理字段
                    }
                    Object value = property.getReadMethod().invoke(obj);
                    if (value != null) {
                        map.put(name, value.toString());
                    }
                }
            } catch (Exception ignored) {
                throw new RuntimeException(ignored);
            }

            // 3. 处理父类
            current = current.getSuperclass();
        }
        return map;
    }

    /**
     * 解析 JSON 字符串为 Map<String, String>
     *
     * @param request
     * @param json JSON 字符串
     * @return Map 表单字段和值
     */
    private Map parseJsonToMap(Request request, String json) throws IOException {
        return request.getSerializer().deserialize(ObjectUtil.objectToByteArrayUniversal(json), Map.class);
    }
}
