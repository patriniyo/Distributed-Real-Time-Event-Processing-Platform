package com.drep.analytics.auth;

import com.drep.common.security.AccessDeniedException;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.Permission;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class SecuritySupport {

    public static final String PRINCIPAL_ATTR = "drep.authenticatedPrincipal";

    private final ApiKeyValidationService validationService;

    public SecuritySupport(ApiKeyValidationService validationService) {
        this.validationService = validationService;
    }

    public AuthenticatedPrincipal authenticate(HttpServletRequest request) {
        Object cached = request.getAttribute(PRINCIPAL_ATTR);
        if (cached instanceof AuthenticatedPrincipal principal) {
            return principal;
        }
        String apiKey = request.getHeader("X-API-Key");
        AuthenticatedPrincipal principal = validationService.requirePrincipal(apiKey);
        request.setAttribute(PRINCIPAL_ATTR, principal);
        return principal;
    }

    public AuthenticatedPrincipal requirePermission(HttpServletRequest request, Permission permission) {
        AuthenticatedPrincipal principal = authenticate(request);
        try {
            principal.requirePermission(permission);
        } catch (AccessDeniedException ex) {
            throw new ForbiddenAuthException(ex.getMessage());
        }
        return principal;
    }

    public AuthenticatedPrincipal requireTenantAccess(HttpServletRequest request,
                                                      Permission permission,
                                                      String tenantId) {
        AuthenticatedPrincipal principal = requirePermission(request, permission);
        try {
            principal.requireTenantAccess(tenantId);
        } catch (AccessDeniedException ex) {
            throw new ForbiddenAuthException(ex.getMessage());
        }
        return principal;
    }
}
