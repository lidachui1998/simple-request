package com.lidachui.simpleRequest.validator;

import com.lidachui.simpleRequest.resolver.Response;

/**
 * DefaultResponseValidator
 *
 * @author: lihuijie
 * @date: 2024/11/19 17:37
 * @version: 1.0
 */
public class DefaultResponseValidator implements ResponseValidator {

    @Override
    public ValidationResult validate(Response response) {
        return new ValidationResult(true, "");
    }
}
