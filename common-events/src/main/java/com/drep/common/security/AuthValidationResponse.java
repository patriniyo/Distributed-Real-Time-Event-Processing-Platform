package com.drep.common.security;

import java.util.UUID;

public record AuthValidationResponse(
        boolean valid,
        UUID keyId,
        String tenantId,
        Role role,
        ApiKeyScope scope,
        boolean platformAdmin,
        String message
) {
    public static AuthValidationResponse invalid(String message) {
        return new AuthValidationResponse(false, null, null, null, null, false, message);
    }

    public AuthenticatedPrincipal toPrincipal() {
        if (!valid) {
            throw new IllegalStateException("Cannot convert invalid auth response");
        }
        return new AuthenticatedPrincipal(keyId, tenantId, role, scope, platformAdmin);
    }
}
