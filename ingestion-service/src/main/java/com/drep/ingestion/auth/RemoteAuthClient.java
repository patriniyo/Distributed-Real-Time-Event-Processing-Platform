package com.drep.ingestion.auth;

import com.drep.common.security.AuthValidationResponse;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.ingestion.config.IngestionProperties;
import com.drep.ingestion.exception.UnauthorizedException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RemoteAuthClient {

    private final AuthClientProperties properties;
    private final IngestionProperties ingestionProperties;
    private final RestTemplate restTemplate;
    private final Map<String, AuthValidationResponse> cache = new ConcurrentHashMap<>();

    public RemoteAuthClient(AuthClientProperties properties,
                            IngestionProperties ingestionProperties,
                            RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.ingestionProperties = ingestionProperties;
        this.restTemplate = restTemplateBuilder.build();
    }

    public AuthenticatedPrincipal authenticateIngest(String apiKey) {
        AuthValidationResponse response = validate(apiKey);
        if (!response.valid()) {
            throw new UnauthorizedException(response.message());
        }
        AuthenticatedPrincipal principal = response.toPrincipal();
        principal.requirePermission(com.drep.common.security.Permission.INGEST_EVENTS);
        if (principal.isPlatformAdmin()) {
            throw new UnauthorizedException("Platform admin key cannot ingest events");
        }
        return principal;
    }

    public AuthenticatedPrincipal authenticateAdmin(String apiKey) {
        AuthValidationResponse response = validate(apiKey);
        if (!response.valid()) {
            throw new UnauthorizedException(response.message());
        }
        return response.toPrincipal();
    }

    private AuthValidationResponse validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return AuthValidationResponse.invalid("Missing authentication: provide X-API-Key or Bearer JWT");
        }

        if (!properties.isEnabled()) {
            return legacyValidate(apiKey);
        }

        AuthValidationResponse cached = cache.get(apiKey);
        if (cached != null) {
            return cached;
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
            if (response != null && response.valid()) {
                cache.put(apiKey, response);
            }
            return response != null ? response : AuthValidationResponse.invalid("Auth service unavailable");
        } catch (RestClientException ex) {
            return AuthValidationResponse.invalid("Auth service unavailable");
        }
    }

    private AuthValidationResponse legacyValidate(String apiKey) {
        String tenantId = ingestionProperties.getApiKeys().get(apiKey);
        if (tenantId != null) {
            return new AuthValidationResponse(
                    true, null, tenantId,
                    com.drep.common.security.Role.ENGINEER,
                    com.drep.common.security.ApiKeyScope.INGEST,
                    false, null
            );
        }
        if (apiKey.equals(properties.getLegacyAdminApiKey())) {
            return new AuthValidationResponse(
                    true, null, AuthenticatedPrincipal.PLATFORM_TENANT,
                    com.drep.common.security.Role.ADMIN,
                    com.drep.common.security.ApiKeyScope.ADMIN,
                    true, null
            );
        }
        return AuthValidationResponse.invalid("Invalid API key");
    }

    public void invalidateCache() {
        cache.clear();
    }
}
