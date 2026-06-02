package com.drep.analytics.controller;

import com.drep.analytics.auth.AuditService;
import com.drep.analytics.auth.SecuritySupport;
import com.drep.analytics.auth.TenantProvisioningService;
import com.drep.common.security.Permission;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/tenants/provision")
public class TenantProvisioningController {

    private final TenantProvisioningService tenantProvisioningService;
    private final SecuritySupport securitySupport;

    public TenantProvisioningController(TenantProvisioningService tenantProvisioningService,
                                        SecuritySupport securitySupport) {
        this.tenantProvisioningService = tenantProvisioningService;
        this.securitySupport = securitySupport;
    }

    @GetMapping
    public List<TenantProvisioningService.TenantSummary> list(HttpServletRequest request) {
        securitySupport.requirePermission(request, Permission.ADMIN_TENANT_PROVISION);
        return tenantProvisioningService.list();
    }

    @PostMapping("/{tenantId}")
    public ResponseEntity<TenantProvisioningService.TenantSummary> create(@PathVariable String tenantId,
                                                                          HttpServletRequest request) {
        var actor = securitySupport.requirePermission(request, Permission.ADMIN_TENANT_PROVISION);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantProvisioningService.create(tenantId, actor));
    }

    @PostMapping("/{tenantId}/suspend")
    public TenantProvisioningService.TenantSummary suspend(@PathVariable String tenantId,
                                                           HttpServletRequest request) {
        var actor = securitySupport.requirePermission(request, Permission.ADMIN_TENANT_PROVISION);
        return tenantProvisioningService.suspend(tenantId, actor);
    }

    @DeleteMapping("/{tenantId}")
    public TenantProvisioningService.TenantSummary delete(@PathVariable String tenantId,
                                                          HttpServletRequest request) {
        var actor = securitySupport.requirePermission(request, Permission.ADMIN_TENANT_PROVISION);
        return tenantProvisioningService.delete(tenantId, actor);
    }
}
