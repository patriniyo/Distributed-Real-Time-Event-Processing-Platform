package com.drep.ingestion.controller;

import com.drep.common.security.Permission;
import com.drep.ingestion.auth.IngestionSecuritySupport;
import com.drep.ingestion.config.IngestionProperties;
import com.drep.ingestion.dto.TenantRateLimitDto;
import com.drep.ingestion.dto.TenantRateLimitUpdateRequest;
import com.drep.ingestion.service.TenantRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/tenants")
public class TenantAdminController {

    private final IngestionProperties properties;
    private final TenantRateLimitService rateLimitService;
    private final IngestionSecuritySupport securitySupport;

    public TenantAdminController(IngestionProperties properties,
                                 TenantRateLimitService rateLimitService,
                                 IngestionSecuritySupport securitySupport) {
        this.properties = properties;
        this.rateLimitService = rateLimitService;
        this.securitySupport = securitySupport;
    }

    @GetMapping
    public List<TenantRateLimitDto> list(HttpServletRequest request) {
        securitySupport.requirePermission(request, Permission.ADMIN_TENANT_CONFIG);
        List<TenantRateLimitDto> tenants = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : properties.getTenantRateLimits().entrySet()) {
            tenants.add(new TenantRateLimitDto(entry.getKey(), entry.getValue()));
        }
        return tenants;
    }

    @GetMapping("/{tenantId}")
    public TenantRateLimitDto get(@PathVariable String tenantId, HttpServletRequest request) {
        securitySupport.requireTenantAccess(request, Permission.ADMIN_TENANT_CONFIG, tenantId);
        return new TenantRateLimitDto(tenantId, rateLimitService.getLimitForTenant(tenantId));
    }

    @PutMapping("/{tenantId}")
    public TenantRateLimitDto update(@PathVariable String tenantId,
                                     @Valid @RequestBody TenantRateLimitUpdateRequest body,
                                     HttpServletRequest request) {
        securitySupport.requireTenantAccess(request, Permission.ADMIN_TENANT_CONFIG, tenantId);
        properties.getTenantRateLimits().put(tenantId, body.rateLimitRps());
        return new TenantRateLimitDto(tenantId, body.rateLimitRps());
    }
}
