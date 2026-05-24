package com.drep.ingestion.controller;

import com.drep.ingestion.dto.ErrorResponse;

import java.util.List;

public class ValidationFailedException extends RuntimeException {

    private final List<ErrorResponse.FieldError> fieldErrors;

    public ValidationFailedException(List<ErrorResponse.FieldError> fieldErrors) {
        super("Request validation failed");
        this.fieldErrors = fieldErrors;
    }

    public List<ErrorResponse.FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
