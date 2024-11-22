package com.lidachui.simpleRequest.serialize;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Serializer
 *
 * @author: lihuijie
 * @date: 2024/11/22 23:04
 * @version: 1.0
 */
public interface Serializer {
    /**
     * 序列化
     *
     * @param input 输入
     * @return 一串
     */
    String serialize(Object input);

    /**
     * 反序列化
     *
     * @param input 输入
     * @param type 类型
     * @return t
     */
    <T> T deserialize(String input, Class<T> type);

    /**
     * 反序列化
     *
     * @param input 输入
     * @param typeReference 类型参考
     * @return t
     */
    <T> T deserialize(String input, TypeReference<T> typeReference);
}
