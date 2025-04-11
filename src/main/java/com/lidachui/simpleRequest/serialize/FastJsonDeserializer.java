package com.lidachui.simpleRequest.serialize;


import com.alibaba.fastjson.JSON;

import java.lang.reflect.Type;

/**
 * FastJsonDeserializer
 *
 * @author: lihuijie
 * @date: 2025/1/6 10:37
 * @version: 1.0
 */
public class FastJsonDeserializer implements Serializer{

  /**
   * 序列化
   *
   * @param input 输入
   * @return 一串
   */
  @Override
  public String serialize(Object input) {
        return JSON.toJSONString(input);
  }

  /**
   * 反序列化
   *
   * @param input        输入
   * @param responseType 类型参考
   * @return t
   */
  @Override
  public <T> T deserialize(String input, Type responseType) {
        return JSON.parseObject(input, responseType);
  }
}
