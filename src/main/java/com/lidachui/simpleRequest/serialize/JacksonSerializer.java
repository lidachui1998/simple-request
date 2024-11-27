package com.lidachui.simpleRequest.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * DefaultSerializer
 *
 * @author: lihuijie
 * @date: 2024/11/22 23:04
 * @version: 1.0
 */
public class JacksonSerializer implements Serializer {

    public static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 序列化
     *
     * @param input 输入
     * @return 一串
     */
    @Override
    public String serialize(Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    /**
     * 反序列化
     *
     * @param input 输入
     * @param type 类型
     * @return t
     */
    @Override
    public <T> T deserialize(String input, Class<T> type) {
        try {
            return objectMapper.readValue(input, type);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    /**
     * 反序列化
     *
     * @param input 输入
     * @param responseType 类型参考
     * @return t
     */
    @Override
    public <T> T deserialize(String input, Type responseType) {
        try {
            // 获取 ObjectMapper 的 JsonParser 实例
            JsonParser parser = objectMapper.getFactory().createParser(input);

            // 获取响应类型的 JavaType（缓存 JavaType）
            JavaType javaType = getCachedJavaType(responseType);

            // 使用流式解析而不是完全解析为树形结构
            return parseNode(parser, javaType);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    private <T> T parseNode(JsonParser parser, JavaType targetType) throws IOException {
        JsonToken currentToken = parser.nextToken();

        // 检查当前 token 类型
        if (currentToken == JsonToken.START_OBJECT) {
            // 如果是对象，直接映射
            JsonNode rootNode = objectMapper.readTree(parser);
            return objectMapper.treeToValue(rootNode, targetType);
        } else if (currentToken == JsonToken.START_ARRAY) {
            // 如果是数组，递归解析
            List<Object> result = new ArrayList<>();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                result.add(parseNode(parser, targetType));
            }
            return (T) result;
        } else {
            // 其他类型（如基本类型），直接返回转换后的对象
            return objectMapper.readValue(parser, targetType);
        }
    }

    // 缓存 JavaType，减少反射调用
    private JavaType getCachedJavaType(Type responseType) {
        return objectMapper.getTypeFactory().constructType(responseType);
    }

}
