package com.drep.ingestion.controller;

import com.drep.common.dto.ApiErrorResponse;

import java.util.List;

public class ValidationFailedException extends RuntimeException {

    private final List<ApiErrorResponse.FieldError> fieldErrors;

    public ValidationFailedException(List<ApiErrorResponse.FieldError> fieldErrors) {
        super("Request validation failed");
        this.fieldErrors = fieldErrors;
    }

    public List<ApiErrorResponse.FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
