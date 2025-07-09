package com.lidachui.simpleRequest.serialize;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.lang.reflect.Type;


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
    }

    @Override
    public String serialize(Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] input, Type responseType) {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(responseType);
            return objectMapper.readValue(input, javaType);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }
}

