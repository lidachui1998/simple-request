package com.lidachui.simpleRequest.mock;

import java.lang.reflect.Type;

/**
 * MockGenerator
 *
 * @author: lihuijie
 * @date: 2024/12/4 23:06
 * @version: 1.0
 */
public interface MockGenerator {

    /**
     * 生成模拟数据
     *
     * @param type 类型
     * @return 对象
     */
    <T> T generate(Type type);
}
