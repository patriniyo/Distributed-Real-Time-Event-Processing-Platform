package com.drep.analytics.auth;

import com.drep.analytics.config.AuthProperties;
import com.drep.analytics.domain.ApiKeyEntity;
import com.drep.analytics.domain.TenantEntity;
import com.drep.analytics.repository.ApiKeyRepository;
import com.drep.analytics.repository.TenantRepository;
import com.drep.common.security.ApiKeyScope;
import com.drep.common.security.AuthValidationResponse;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.Role;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyValidationService {

    private final AuthProperties authProperties;
    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;

    public ApiKeyValidationService(AuthProperties authProperties,
                                   ApiKeyRepository apiKeyRepository,
                                   TenantRepository tenantRepository) {
        this.authProperties = authProperties;
        this.apiKeyRepository = apiKeyRepository;
        this.tenantRepository = tenantRepository;
    }

    public AuthValidationResponse validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return AuthValidationResponse.invalid("API key required");
        }

        if (!authProperties.isEnabled()) {
            return legacyValidate(apiKey);
        }

        String hash = ApiKeyHasher.hash(apiKey);
        return apiKeyRepository.findByKeyHashAndActiveTrue(hash)
                .map(this::toResponse)
                .orElseGet(() -> AuthValidationResponse.invalid("Invalid API key"));
    }

    public AuthenticatedPrincipal requirePrincipal(String apiKey) {
        AuthValidationResponse response = validate(apiKey);
        if (!response.valid()) {
            throw new UnauthorizedAuthException(response.message());
        }
        return response.toPrincipal();
    }

    private AuthValidationResponse toResponse(ApiKeyEntity entity) {
        TenantEntity tenant = tenantRepository.findById(entity.getTenantId()).orElse(null);
        if (tenant == null || !"ACTIVE".equals(tenant.getStatus())) {
            return AuthValidationResponse.invalid("Tenant is not active");
        }

        boolean platformAdmin = AuthenticatedPrincipal.PLATFORM_TENANT.equals(entity.getTenantId())
                && Role.ADMIN.name().equals(entity.getRole());

        return new AuthValidationResponse(
                true,
                entity.getId(),
                entity.getTenantId(),
                Role.valueOf(entity.getRole()),
                ApiKeyScope.valueOf(entity.getScope()),
                platformAdmin,
                null
        );
    }

    private AuthValidationResponse legacyValidate(String apiKey) {
        if (apiKey.equals(authProperties.getLegacyAdminApiKey())) {
            return new AuthValidationResponse(
                    true,
                    null,
                    AuthenticatedPrincipal.PLATFORM_TENANT,
                    Role.ADMIN,
                    ApiKeyScope.ADMIN,
                    true,
                    null
            );
        }
        return AuthValidationResponse.invalid("Invalid API key");
    }
}
