package com.drep.common.security;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedPrincipal(
        UUID keyId,
        String tenantId,
        Role role,
        ApiKeyScope scope,
        boolean platformAdmin
) {
    public static final String PLATFORM_TENANT = "__platform__";

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public void requirePermission(Permission permission) {
        if (!RbacMatrix.isAllowed(this, permission)) {
            throw new AccessDeniedException("Missing permission: " + permission);
        }
    }

    public void requireTenantAccess(String requestedTenant) {
        if (isPlatformAdmin()) {
            return;
        }
        if (requestedTenant == null || !tenantId.equals(requestedTenant)) {
            throw new AccessDeniedException("Access denied for tenant: " + requestedTenant);
        }
    }
}
