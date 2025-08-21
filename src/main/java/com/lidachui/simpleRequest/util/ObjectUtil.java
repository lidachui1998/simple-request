package com.lidachui.simpleRequest.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;


import java.util.*;


/**
 * ObjectUtil
 *
 * @author: lihuijie
 * @date: 2025/5/29 14:32
 * @version: 1.0
 */
public class ObjectUtil {


    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 完全兼容的对象到字节数组转换
     *
     * @param obj 任意对象
     * @return byte[]
     * @throws IOException IOException
     */
    public static byte[] objectToByteArrayUniversal(Object obj) throws IOException {
        if (obj == null) {
            return new byte[0];
        }

        // 1. 如果已经是字节数组，直接返回
        if (obj instanceof byte[]) {
            return (byte[]) obj;
        }

        // 2. 基本类型和包装类型
        if (isPrimitiveOrWrapper(obj)) {
            return obj.toString().getBytes(StandardCharsets.UTF_8);
        }

        // 3. 字符串类型
        if (obj instanceof String) {
            return ((String) obj).getBytes(StandardCharsets.UTF_8);
        }

        // 4. 集合类型
        if (obj instanceof Collection || obj instanceof Map || obj.getClass().isArray()) {
            return handleCollectionOrArray(obj);
        }

        // 5. 尝试JSON序列化
        try {
            return jsonSerialize(obj);
        } catch (Exception e) {
            // 6. JSON序列化失败，尝试Java原生序列化
            try {
                return javaSerialize(obj);
            } catch (Exception ex) {
                // 7. 都失败了，转为字符串
                return obj.toString().getBytes(StandardCharsets.UTF_8);
            }
        }
    }

    /**
     * 检查是否为基本类型或包装类型
     */
    private static boolean isPrimitiveOrWrapper(Object obj) {
        return obj instanceof Number ||
                obj instanceof Boolean ||
                obj instanceof Character ||
                obj.getClass().isPrimitive();
    }

    /**
     * 处理集合、数组、Map等类型
     */
    private static byte[] handleCollectionOrArray(Object obj) throws IOException {
        try {
            // 优先使用JSON序列化集合类型
            return jsonSerialize(obj);
        } catch (Exception e) {
            // JSON序列化失败，尝试Java原生序列化
            return javaSerialize(obj);
        }
    }

    /**
     * JSON序列化
     */
    private static byte[] jsonSerialize(Object obj) throws JsonProcessingException {
        String jsonString = objectMapper.writeValueAsString(obj);
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Java原生序列化
     */
    private static byte[] javaSerialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        }
    }

    /**
     * 创建Bean实例
     *
     * @param beanClass Bean类
     * @return Bean实例
     */
    public static <T> T createInstance(Class<T> beanClass) {
        try {
            return beanClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + beanClass.getName(), e);
        }
    }
}
