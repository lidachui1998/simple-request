package com.lidachui.simpleRequest.serialize;


import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
     * @param responseType 类型参考
     * @return t
     */
    @Override
    public <T> T deserialize(byte[] input, Type responseType) {
        try {
            TreeNode treeNode = objectMapper.readTree(input);
            return objectMapper.treeToValue(treeNode, objectMapper.constructType(responseType));
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }
}
