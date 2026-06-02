package com.drep.dashboard.controller;

import com.drep.common.security.AccessDeniedException;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.Permission;
import com.drep.dashboard.auth.RemoteAuthClient;
import com.drep.dashboard.auth.UnauthorizedAuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ApiAuthHelper {

    public static final String PRINCIPAL_ATTR = "drep.authenticatedPrincipal";

    private final RemoteAuthClient remoteAuthClient;

    public ApiAuthHelper(RemoteAuthClient remoteAuthClient) {
        this.remoteAuthClient = remoteAuthClient;
    }

    public AuthenticatedPrincipal requireAuth(HttpServletRequest request) {
        return authenticate(request);
    }

    public AuthenticatedPrincipal requirePermission(HttpServletRequest request, Permission permission) {
        AuthenticatedPrincipal principal = authenticate(request);
        try {
            principal.requirePermission(permission);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        }
        return principal;
    }

    private AuthenticatedPrincipal authenticate(HttpServletRequest request) {
        Object cached = request.getAttribute(PRINCIPAL_ATTR);
        if (cached instanceof AuthenticatedPrincipal principal) {
            return principal;
        }
        try {
            AuthenticatedPrincipal principal = remoteAuthClient.validate(request.getHeader("X-API-Key"));
            request.setAttribute(PRINCIPAL_ATTR, principal);
            return principal;
        } catch (UnauthorizedAuthException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }
}
