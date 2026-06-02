package com.drep.dashboard.auth;

import com.drep.common.security.AuthValidationResponse;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.ApiKeyScope;
import com.drep.common.security.Role;
import com.drep.dashboard.config.DashboardProperties;
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

    private final AuthClientProperties authClientProperties;
    private final DashboardProperties dashboardProperties;
    private final RestTemplate restTemplate;
    private final Map<String, AuthValidationResponse> cache = new ConcurrentHashMap<>();

    public RemoteAuthClient(AuthClientProperties authClientProperties,
                            DashboardProperties dashboardProperties,
                            RestTemplateBuilder restTemplateBuilder) {
        this.authClientProperties = authClientProperties;
        this.dashboardProperties = dashboardProperties;
        this.restTemplate = restTemplateBuilder.build();
    }

    public AuthenticatedPrincipal validate(String apiKey) {
        AuthValidationResponse response = resolve(apiKey);
        if (!response.valid()) {
            throw new UnauthorizedAuthException(response.message());
        }
        return response.toPrincipal();
    }

    private AuthValidationResponse resolve(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return AuthValidationResponse.invalid("API key required");
        }
        if (!authClientProperties.isEnabled()) {
            if (apiKey.equals(dashboardProperties.getApiKey()) || apiKey.equals(authClientProperties.getLegacyAdminApiKey())) {
                return new AuthValidationResponse(
                        true, null, AuthenticatedPrincipal.PLATFORM_TENANT,
                        Role.ADMIN, ApiKeyScope.ADMIN, true, null);
            }
            return AuthValidationResponse.invalid("API key required");
        }
        AuthValidationResponse cached = cache.get(apiKey);
        if (cached != null) {
            return cached;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", authClientProperties.getInternalToken());
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("apiKey", apiKey), headers);
            AuthValidationResponse response = restTemplate.postForObject(
                    authClientProperties.getAnalyticsUrl() + "/internal/auth/validate",
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
}
