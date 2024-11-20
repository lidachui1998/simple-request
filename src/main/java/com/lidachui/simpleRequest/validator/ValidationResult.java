package com.lidachui.simpleRequest.validator;

/**
 * ValidationResult
 *
 * @author: lihuijie
 * @date: 2024/11/19 17:35
 * @version: 1.0
 */
public class ValidationResult {
    private boolean valid;
    private String errorMessage;

    public ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
