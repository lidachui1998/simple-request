package com.lidachui.simpleRequest.validator;

import com.lidachui.simpleRequest.resolver.Request;
import com.lidachui.simpleRequest.resolver.Response;

/**
 * ResponseValidator
 *
 * @author: lihuijie
 * @date: 2024/11/19 17:30
 * @version: 1.0
 */
public interface ResponseValidator {

    ValidationResult validate(Response response);

    // 处理校验失败时的操作
    default void onFailure(Request request, Response response, ValidationResult validationResult) {
        // 默认什么都不做，子类可以重写该方法实现自定义逻辑
    }
}
