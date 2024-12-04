package com.lidachui.simpleRequest.annotation;

import com.lidachui.simpleRequest.mock.JavaFakerMockGenerator;
import com.lidachui.simpleRequest.mock.MockGenerator;

import java.lang.annotation.*;

/**
 * Mock
 *
 * @author: lihuijie
 * @date: 2024/12/4 23:05
 * @version: 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Mock {

    /**
     * mock数据生成器
     *
     * @return 类<？ 扩展模拟生成器>
     */
    Class<? extends MockGenerator> mockGenerator() default JavaFakerMockGenerator.class;
}
