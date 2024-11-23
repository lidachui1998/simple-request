package com.lidachui.simpleRequest.serialize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * DefaultSerializer
 *
 * @author: lihuijie
 * @date: 2024/11/22 23:04
 * @version: 1.0
 */
public class JacksonSerializer implements Serializer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
     * @param typeReference 类型参考
     * @return t
     */
    @Override
    public <T> T deserialize(String input, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(input, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }
}
