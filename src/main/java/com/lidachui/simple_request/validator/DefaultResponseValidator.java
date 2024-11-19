package com.lidachui.simple_request.validator;

import org.springframework.stereotype.Component;

/**
 * DefaultResponseValidator
 *
 * @author: lihuijie
 * @date: 2024/11/19 17:37
 * @version: 1.0
 */
public class DefaultResponseValidator implements ResponseValidator {

    @Override
    public ValidationResult validate(Object response) {
        return new ValidationResult(true, "");
    }
}
