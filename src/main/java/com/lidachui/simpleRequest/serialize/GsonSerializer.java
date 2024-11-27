package com.lidachui.simpleRequest.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.lang.reflect.Type;
import java.util.*;

/**
 * GsonSerializer
 *
 * @author: lihuijie
 * @date: 2024/11/27 23:47
 * @version: 1.0
 */
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class GsonSerializer implements Serializer {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateTypeAdapter())  // Register the custom DateTypeAdapter
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();

    /**
     * 序列化
     *
     * @param input 输入
     * @return 一串
     */
    @Override
    public String serialize(Object input) {
        try {
            return gson.toJson(input);
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
            return gson.fromJson(input, type);
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
            // 获取 Gson 的 JsonElement 实例
            JsonElement element = JsonParser.parseString(input);

            // 使用 Gson 的流式解析（相当于 Jackson 的流式解析）
            return parseNode(element, responseType);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    private <T> T parseNode(JsonElement element, Type targetType) {
        // 如果是对象，直接转换
        if (element.isJsonObject()) {
            return gson.fromJson(element, targetType);
        }
        // 如果是数组，递归解析
        else if (element.isJsonArray()) {
            List<Object> result = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                result.add(parseNode(item, targetType));
            }
            return (T) result;
        }
        // 其他类型（如基本类型），直接返回转换后的对象
        return gson.fromJson(element, targetType);
    }

    /**
     * 自定义的日期适配器，处理 Unix 时间戳
     */
    public static class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getTime());
        }

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                // 如果是数字，则当作 Unix 时间戳处理
                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                    return new Date(json.getAsLong());
                }
                // 否则尝试用 ISO8601 解析
                return new Date(json.getAsString());
            } catch (Exception e) {
                throw new JsonParseException("Failed to parse date", e);
            }
        }
    }
}