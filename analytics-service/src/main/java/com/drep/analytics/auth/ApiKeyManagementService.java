package com.drep.analytics.auth;

import com.drep.analytics.domain.ApiKeyEntity;
import com.drep.analytics.repository.ApiKeyRepository;
import com.drep.analytics.repository.TenantRepository;
import com.drep.common.security.ApiKeyScope;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ApiKeyManagementService {

    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public ApiKeyManagementService(ApiKeyRepository apiKeyRepository,
                                   TenantRepository tenantRepository,
                                   AuditService auditService) {
        this.apiKeyRepository = apiKeyRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    public record ApiKeyCreated(UUID id, String apiKey, String keyPrefix, ApiKeyScope scope, Role role) {
    }

    public record ApiKeySummary(UUID id, String keyPrefix, ApiKeyScope scope, Role role, Instant createdAt) {
    }

    @Transactional
    public ApiKeyCreated generate(String tenantId, ApiKeyScope scope, Role role, AuthenticatedPrincipal actor) {
        requireTenantExists(tenantId);
        String plaintext = "drep_" + UUID.randomUUID().toString().replace("-", "");
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setKeyHash(ApiKeyHasher.hash(plaintext));
        entity.setKeyPrefix(plaintext.substring(0, Math.min(12, plaintext.length())));
        entity.setScope(scope.name());
        entity.setRole(role.name());
        entity.setActive(true);
        entity.setCreatedAt(Instant.now());
        apiKeyRepository.save(entity);

        auditService.log(actor, "API_KEY_CREATE", "api-keys/" + entity.getId(),
                "tenant=" + tenantId + ", scope=" + scope + ", role=" + role);

        return new ApiKeyCreated(entity.getId(), plaintext, entity.getKeyPrefix(), scope, role);
    }

    public List<ApiKeySummary> list(String tenantId) {
        return apiKeyRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .map(key -> new ApiKeySummary(
                        key.getId(),
                        key.getKeyPrefix(),
                        ApiKeyScope.valueOf(key.getScope()),
                        Role.valueOf(key.getRole()),
                        key.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void revoke(UUID keyId, AuthenticatedPrincipal actor) {
        ApiKeyEntity entity = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        entity.setActive(false);
        entity.setRevokedAt(Instant.now());
        apiKeyRepository.save(entity);
        auditService.log(actor, "API_KEY_REVOKE", "api-keys/" + keyId, null);
    }

    @Transactional
    public ApiKeyCreated rotate(UUID keyId, AuthenticatedPrincipal actor) {
        ApiKeyEntity existing = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        ApiKeyCreated created = generate(
                existing.getTenantId(),
                ApiKeyScope.valueOf(existing.getScope()),
                Role.valueOf(existing.getRole()),
                actor
        );
        ApiKeyEntity newEntity = apiKeyRepository.findById(created.id()).orElseThrow();
        newEntity.setRotatedFrom(existing.getId());
        apiKeyRepository.save(newEntity);

        existing.setActive(false);
        existing.setRevokedAt(Instant.now());
        apiKeyRepository.save(existing);

        auditService.log(actor, "API_KEY_ROTATE", "api-keys/" + keyId, "newKeyId=" + created.id());
        return created;
    }

    private void requireTenantExists(String tenantId) {
        tenantRepository.findById(tenantId)
                .filter(t -> "ACTIVE".equals(t.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found or inactive"));
    }
}
