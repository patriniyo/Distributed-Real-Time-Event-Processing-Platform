package com.drep.analytics.auth;

public class ForbiddenAuthException extends RuntimeException {

    public ForbiddenAuthException(String message) {
        super(message);
    }
}
