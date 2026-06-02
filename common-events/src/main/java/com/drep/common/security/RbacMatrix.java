package com.drep.common.security;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class RbacMatrix {

    private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS = Map.of(
            Role.ADMIN, EnumSet.allOf(Permission.class),
            Role.ENGINEER, EnumSet.of(
                    Permission.INGEST_EVENTS,
                    Permission.READ_EVENTS,
                    Permission.READ_ANALYTICS,
                    Permission.ADMIN_DLQ,
                    Permission.ADMIN_REPLAY,
                    Permission.ADMIN_TENANT_CONFIG
            ),
            Role.ANALYST, EnumSet.of(
                    Permission.READ_EVENTS,
                    Permission.READ_ANALYTICS
            ),
            Role.READ_ONLY, EnumSet.of(
                    Permission.READ_EVENTS,
                    Permission.READ_ANALYTICS
            )
    );

    private static final Map<ApiKeyScope, Set<Permission>> SCOPE_PERMISSIONS = Map.of(
            ApiKeyScope.INGEST, EnumSet.of(Permission.INGEST_EVENTS),
            ApiKeyScope.READ, EnumSet.of(Permission.READ_EVENTS, Permission.READ_ANALYTICS),
            ApiKeyScope.ADMIN, EnumSet.allOf(Permission.class)
    );

    private RbacMatrix() {
    }

    public static boolean isAllowed(AuthenticatedPrincipal principal, Permission permission) {
        if (principal.isPlatformAdmin()) {
            return true;
        }
        Set<Permission> rolePermissions = ROLE_PERMISSIONS.getOrDefault(principal.role(), Set.of());
        Set<Permission> scopePermissions = SCOPE_PERMISSIONS.getOrDefault(principal.scope(), Set.of());
        return rolePermissions.contains(permission) && scopePermissions.contains(permission);
    }
}
