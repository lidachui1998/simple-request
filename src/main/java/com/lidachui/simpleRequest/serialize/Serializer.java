package com.lidachui.simpleRequest.serialize;

import java.lang.reflect.Type;

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
     * @param responseType 类型参考
     * @return t
     */
    <T> T deserialize(byte[] input, Type responseType);
}
