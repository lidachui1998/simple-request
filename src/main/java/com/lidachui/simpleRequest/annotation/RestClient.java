package com.lidachui.simpleRequest.annotation;

import com.lidachui.simpleRequest.constants.RequestClientType;
import com.lidachui.simpleRequest.serialize.DefaultSerializer;
import com.lidachui.simpleRequest.serialize.Serializer;
import com.lidachui.simpleRequest.validator.DefaultResponseValidator;
import com.lidachui.simpleRequest.validator.ResponseValidator;

import java.lang.annotation.*;

/**
 * RestClient (若baseUrl和propertyKey 同时存在，默认先取propertyKey)
 *
 * @author: lihuijie
 * @date: 2024/11/18 21:49
 * @version: 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestClient {
    /**
     * 基础 URL，支持直接设置
     *
     * @return {@code String }
     */
    String baseUrl() default "";

    /**
     * 指定从配置文件中获取的 key
     *
     * @return {@code String }
     */
    String propertyKey() default "";

    /**
     * 客户端类型
     *
     * @return {@code RequestClientType }
     */
    RequestClientType clientType() default RequestClientType.REST_TEMPLATE;

    /**
     * 响应验证器
     *
     * @return {@code Class<? extends ResponseValidator> }
     */
    Class<? extends ResponseValidator> responseValidator() default DefaultResponseValidator.class;

    /**
     * 名称
     *
     * @return 一串
     */
    String name() default "";

    /**
     * 序列化
     *
     * @return 类<？ 扩展序列化程序>
     */
    Class<? extends Serializer> serializer() default DefaultSerializer.class;
}
