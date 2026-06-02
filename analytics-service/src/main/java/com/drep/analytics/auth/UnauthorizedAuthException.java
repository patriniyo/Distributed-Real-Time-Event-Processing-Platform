package com.drep.analytics.auth;

public class UnauthorizedAuthException extends RuntimeException {

    public UnauthorizedAuthException(String message) {
        super(message);
    }
}
