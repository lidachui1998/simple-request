package com.lidachui.simpleRequest.serialize;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FastJsonSerializer
 *
 * @author: lihuijie
 * @date: 2024/11/28 22:53
 * @version: 1.0
 */
public class FastJsonSerializer implements Serializer {

    /**
     * 序列化
     *
     * @param input 输入
     * @return 一串
     */
    @Override
    public String serialize(Object input) {
        try {
            return JSON.toJSONString(input, SerializerFeature.WriteDateUseDateFormat);
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
            return JSON.parseObject(input, type);
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
            // 将 JSON 转换为 JSONObject 或 JSONArray
            Object parsed = JSON.parse(input, Feature.SupportAutoType);

            if (parsed instanceof JSONObject) {
                return parseNode((JSONObject) parsed, responseType);
            } else if (parsed instanceof JSONArray) {
                return parseNode((JSONArray) parsed, responseType);
            } else {
                throw new RuntimeException("Unsupported JSON type");
            }
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    /**
     * 解析 JSON 对象节点
     */
    private <T> T parseNode(JSONObject node, Type targetType) {
        if (node == null || node.isEmpty()) {
            return null;
        }

        // 遍历 JSON 对象的字段，处理 null 或空对象
        for (Map.Entry<String, Object> field : node.entrySet()) {
            Object value = field.getValue();

            if (value == null || (value instanceof JSONObject && ((JSONObject) value).isEmpty())) {
                node.put(field.getKey(), null);
            }
        }

        return JSON.parseObject(node.toJSONString(), targetType);
    }

    /**
     * 解析 JSON 数组节点
     */
    private <T> T parseNode(JSONArray array, Type targetType) {
        if (array == null || array.isEmpty()) {
            return null;
        }

        List<Object> result = new ArrayList<>();
        for (Object item : array) {
            if (item instanceof JSONObject) {
                result.add(parseNode((JSONObject) item, targetType));
            } else if (item instanceof JSONArray) {
                result.add(parseNode((JSONArray) item, targetType));
            } else {
                result.add(item);
            }
        }

        return (T) result;
    }
}
