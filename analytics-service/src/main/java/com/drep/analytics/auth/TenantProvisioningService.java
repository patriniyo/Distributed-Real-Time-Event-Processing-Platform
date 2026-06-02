package com.drep.analytics.auth;

import com.drep.analytics.domain.TenantEntity;
import com.drep.analytics.repository.TenantRepository;
import com.drep.common.security.AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public TenantProvisioningService(TenantRepository tenantRepository, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    public record TenantSummary(String tenantId, String status, Instant createdAt) {
    }

    public List<TenantSummary> list() {
        return tenantRepository.findAll().stream()
                .filter(t -> !AuthenticatedPrincipal.PLATFORM_TENANT.equals(t.getTenantId()))
                .map(t -> new TenantSummary(t.getTenantId(), t.getStatus(), t.getCreatedAt()))
                .toList();
    }

    @Transactional
    public TenantSummary create(String tenantId, AuthenticatedPrincipal actor) {
        if (tenantRepository.existsById(tenantId)) {
            throw new IllegalArgumentException("Tenant already exists");
        }
        TenantEntity entity = new TenantEntity();
        entity.setTenantId(tenantId);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        tenantRepository.save(entity);
        auditService.log(actor, "TENANT_CREATE", "tenants/" + tenantId, null);
        return new TenantSummary(entity.getTenantId(), entity.getStatus(), entity.getCreatedAt());
    }

    @Transactional
    public TenantSummary suspend(String tenantId, AuthenticatedPrincipal actor) {
        TenantEntity entity = requireTenant(tenantId);
        entity.setStatus("SUSPENDED");
        entity.setUpdatedAt(Instant.now());
        tenantRepository.save(entity);
        auditService.log(actor, "TENANT_SUSPEND", "tenants/" + tenantId, null);
        return new TenantSummary(entity.getTenantId(), entity.getStatus(), entity.getCreatedAt());
    }

    @Transactional
    public TenantSummary delete(String tenantId, AuthenticatedPrincipal actor) {
        TenantEntity entity = requireTenant(tenantId);
        entity.setStatus("DELETED");
        entity.setUpdatedAt(Instant.now());
        tenantRepository.save(entity);
        auditService.log(actor, "TENANT_DELETE", "tenants/" + tenantId, null);
        return new TenantSummary(entity.getTenantId(), entity.getStatus(), entity.getCreatedAt());
    }

    private TenantEntity requireTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .filter(t -> !AuthenticatedPrincipal.PLATFORM_TENANT.equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
    }
}
