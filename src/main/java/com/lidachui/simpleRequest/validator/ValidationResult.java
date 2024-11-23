package com.lidachui.simpleRequest.validator;

import lombok.Getter;

/**
 * ValidationResult
 *
 * @author: lihuijie
 * @date: 2024/11/19 17:35
 * @version: 1.0
 */
@Getter
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;

    public ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

}
