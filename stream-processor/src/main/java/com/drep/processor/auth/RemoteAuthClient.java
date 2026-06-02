package com.drep.processor.auth;

import com.drep.common.security.AuthValidationResponse;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.ApiKeyScope;
import com.drep.common.security.Role;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class RemoteAuthClient {

    private final AuthClientProperties properties;
    private final RestTemplate restTemplate;

    public RemoteAuthClient(AuthClientProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
    }

    public AuthenticatedPrincipal requireAdmin(String apiKey) {
        AuthValidationResponse response = validate(apiKey);
        if (!response.valid()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, response.message());
        }
        return response.toPrincipal();
    }

    private AuthValidationResponse validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return AuthValidationResponse.invalid("Admin API key required");
        }
        if (!properties.isEnabled()) {
            if (apiKey.equals(properties.getLegacyAdminApiKey())) {
                return new AuthValidationResponse(
                        true, null, AuthenticatedPrincipal.PLATFORM_TENANT,
                        Role.ADMIN, ApiKeyScope.ADMIN, true, null);
            }
            return AuthValidationResponse.invalid("Admin API key required");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", properties.getInternalToken());
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("apiKey", apiKey), headers);
            AuthValidationResponse response = restTemplate.postForObject(
                    properties.getAnalyticsUrl() + "/internal/auth/validate",
                    entity,
                    AuthValidationResponse.class
            );
            return response != null ? response : AuthValidationResponse.invalid("Auth service unavailable");
        } catch (RestClientException ex) {
            return AuthValidationResponse.invalid("Auth service unavailable");
        }
    }
}
