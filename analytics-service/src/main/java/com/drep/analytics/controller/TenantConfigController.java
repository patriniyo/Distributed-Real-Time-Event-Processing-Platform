package com.drep.analytics.controller;

import com.drep.analytics.auth.AuditService;
import com.drep.analytics.auth.SecuritySupport;
import com.drep.analytics.config.AnalyticsProperties;
import com.drep.analytics.domain.TenantRetentionEntity;
import com.drep.analytics.dto.TenantRetentionDto;
import com.drep.analytics.dto.TenantRetentionUpdateRequest;
import com.drep.analytics.repository.TenantRetentionRepository;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.Permission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/tenants")
public class TenantConfigController {

    private final TenantRetentionRepository tenantRetentionRepository;
    private final AnalyticsProperties properties;
    private final SecuritySupport securitySupport;
    private final AuditService auditService;

    public TenantConfigController(TenantRetentionRepository tenantRetentionRepository,
                                  AnalyticsProperties properties,
                                  SecuritySupport securitySupport,
                                  AuditService auditService) {
        this.tenantRetentionRepository = tenantRetentionRepository;
        this.properties = properties;
        this.securitySupport = securitySupport;
        this.auditService = auditService;
    }

    @GetMapping
    public List<TenantRetentionDto> list(HttpServletRequest request) {
        securitySupport.requirePermission(request, Permission.ADMIN_TENANT_CONFIG);
        return tenantRetentionRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{tenantId}")
    public TenantRetentionDto get(@PathVariable String tenantId, HttpServletRequest request) {
        securitySupport.requireTenantAccess(request, Permission.ADMIN_TENANT_CONFIG, tenantId);
        return tenantRetentionRepository.findById(tenantId)
                .map(this::toDto)
                .orElseGet(() -> new TenantRetentionDto(tenantId, properties.getRetention().getDefaultDays()));
    }

    @PutMapping("/{tenantId}")
    public TenantRetentionDto update(@PathVariable String tenantId,
                                     @Valid @RequestBody TenantRetentionUpdateRequest body,
                                     HttpServletRequest request) {
        AuthenticatedPrincipal actor = securitySupport.requireTenantAccess(
                request, Permission.ADMIN_TENANT_CONFIG, tenantId);
        TenantRetentionEntity entity = tenantRetentionRepository.findById(tenantId)
                .orElseGet(() -> {
                    TenantRetentionEntity created = new TenantRetentionEntity();
                    created.setTenantId(tenantId);
                    return created;
                });
        entity.setRetentionDays(body.retentionDays());
        tenantRetentionRepository.save(entity);
        properties.getTenantRetentionDays().put(tenantId, body.retentionDays());
        auditService.log(actor, "TENANT_RETENTION_UPDATE", "tenants/" + tenantId,
                "retentionDays=" + body.retentionDays());
        return toDto(entity);
    }

    private TenantRetentionDto toDto(TenantRetentionEntity entity) {
        return new TenantRetentionDto(entity.getTenantId(), entity.getRetentionDays());
    }
}
