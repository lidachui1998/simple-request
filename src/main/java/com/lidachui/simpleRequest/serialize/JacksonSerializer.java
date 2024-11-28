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
import java.util.Map.Entry;

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

            // 获取响应类型的 JavaType
            JavaType javaType = objectMapper.getTypeFactory().constructType(responseType);

            // 遍历 JSON 内容，手动解析每个节点
            JsonNode rootNode = parser.readValueAsTree(); // 将输入的 JSON 字符串转为 JsonNode
            return parseNode(rootNode, javaType); // 递归或逐个解析每个节点
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    // 解析根节点或特定节点的辅助方法
    private <T> T parseNode(JsonNode node, JavaType targetType) {
        // 处理空节点或者空对象
        if (node.isNull() || (node.isObject() && node.size() == 0)) {
            return null;
        }

        // 处理对象类型，检查每个字段
        if (node.isObject()) {
            Iterator<Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode fieldValue = field.getValue();

                // 如果子字段是空对象或者 null，设置为 null
                if (fieldValue.isNull() || (fieldValue.isObject() && fieldValue.size() == 0)) {
                    ((ObjectNode) node).set(field.getKey(), NullNode.getInstance()); // 设置为空
                }
            }
            return objectMapper.convertValue(node, targetType);
        }

        // 处理数组类型，递归解析每个元素
        if (node.isArray()) {
            List<Object> result = new ArrayList<>();
            for (JsonNode arrayNode : node) {
                result.add(parseNode(arrayNode, targetType));
            }
            return (T) result;
        }

        // 处理基础类型（如字符串、数字等）
        return objectMapper.convertValue(node, targetType);
    }
}
