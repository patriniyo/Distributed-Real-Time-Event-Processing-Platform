package com.drep.dashboard.controller;

import com.drep.dashboard.config.DashboardProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ApiAuthHelper {

    private final DashboardProperties properties;

    public ApiAuthHelper(DashboardProperties properties) {
        this.properties = properties;
    }

    public void requireAuth(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || !apiKey.equals(properties.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key required");
        }
    }
}
