package com.drep.dashboard.auth;

public class UnauthorizedAuthException extends RuntimeException {

    public UnauthorizedAuthException(String message) {
        super(message);
    }
}
