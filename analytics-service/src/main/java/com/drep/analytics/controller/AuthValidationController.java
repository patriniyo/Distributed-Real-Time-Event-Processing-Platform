package com.drep.analytics.controller;

import com.drep.analytics.auth.ApiKeyValidationService;
import com.drep.analytics.config.AuthProperties;
import com.drep.common.security.AuthValidationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/internal/auth")
public class AuthValidationController {

    private final ApiKeyValidationService validationService;
    private final AuthProperties authProperties;

    public AuthValidationController(ApiKeyValidationService validationService, AuthProperties authProperties) {
        this.validationService = validationService;
        this.authProperties = authProperties;
    }

    @PostMapping("/validate")
    public AuthValidationResponse validate(@RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!authProperties.getInternalToken().equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
        return validationService.validate(body.get("apiKey"));
    }
}
