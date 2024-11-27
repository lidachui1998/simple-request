package com.lidachui.simpleRequest.handler;

import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.Response;
import com.lidachui.simpleRequest.serialize.JacksonSerializer;
import com.lidachui.simpleRequest.serialize.Serializer;

import kotlin.Pair;

import okhttp3.*;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
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
                    .equalsIgnoreCase(headers.getOrDefault("Content-Type", ""))) {
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
                formFields = parseJsonToMap((String) body);
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
            }

            // 3. 处理父类
            current = current.getSuperclass();
        }
        return map;
    }

    /**
     * 解析 JSON 字符串为 Map<String, String>
     *
     * @param json JSON 字符串
     * @return Map 表单字段和值
     */
    private Map parseJsonToMap(String json) {
        JacksonSerializer jacksonSerializer = new JacksonSerializer();
        return jacksonSerializer.deserialize(json, Map.class);
    }
}
