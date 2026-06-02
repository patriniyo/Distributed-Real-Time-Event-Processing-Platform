package com.drep.analytics.controller;

import com.drep.analytics.auth.ApiKeyManagementService;
import com.drep.analytics.auth.SecuritySupport;
import com.drep.common.security.ApiKeyScope;
import com.drep.common.security.Permission;
import com.drep.common.security.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/tenants/{tenantId}/api-keys")
public class ApiKeyManagementController {

    private final ApiKeyManagementService apiKeyManagementService;
    private final SecuritySupport securitySupport;

    public ApiKeyManagementController(ApiKeyManagementService apiKeyManagementService,
                                      SecuritySupport securitySupport) {
        this.apiKeyManagementService = apiKeyManagementService;
        this.securitySupport = securitySupport;
    }

    @GetMapping
    public List<ApiKeyManagementService.ApiKeySummary> list(@PathVariable String tenantId,
                                                            HttpServletRequest request) {
        securitySupport.requireTenantAccess(request, Permission.ADMIN_API_KEYS, tenantId);
        return apiKeyManagementService.list(tenantId);
    }

    @PostMapping
    public ResponseEntity<ApiKeyManagementService.ApiKeyCreated> create(@PathVariable String tenantId,
                                                                        @RequestBody Map<String, String> body,
                                                                        HttpServletRequest request) {
        var actor = securitySupport.requireTenantAccess(request, Permission.ADMIN_API_KEYS, tenantId);
        ApiKeyScope scope = ApiKeyScope.valueOf(body.getOrDefault("scope", "READ"));
        Role role = Role.valueOf(body.getOrDefault("role", "READ_ONLY"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyManagementService.generate(tenantId, scope, role, actor));
    }

    @DeleteMapping("/{keyId}")
    public void revoke(@PathVariable String tenantId,
                       @PathVariable UUID keyId,
                       HttpServletRequest request) {
        var actor = securitySupport.requireTenantAccess(request, Permission.ADMIN_API_KEYS, tenantId);
        apiKeyManagementService.revoke(keyId, actor);
    }

    @PostMapping("/{keyId}/rotate")
    public ApiKeyManagementService.ApiKeyCreated rotate(@PathVariable String tenantId,
                                                        @PathVariable UUID keyId,
                                                        HttpServletRequest request) {
        var actor = securitySupport.requireTenantAccess(request, Permission.ADMIN_API_KEYS, tenantId);
        return apiKeyManagementService.rotate(keyId, actor);
    }
}
