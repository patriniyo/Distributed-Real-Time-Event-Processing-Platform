package com.drep.ingestion.auth;

import com.drep.common.security.AccessDeniedException;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.Permission;
import com.drep.ingestion.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class IngestionSecuritySupport {

    public static final String PRINCIPAL_ATTR = "drep.authenticatedPrincipal";

    private final RemoteAuthClient remoteAuthClient;

    public IngestionSecuritySupport(RemoteAuthClient remoteAuthClient) {
        this.remoteAuthClient = remoteAuthClient;
    }

    public AuthenticatedPrincipal requireAdmin(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        try {
            AuthenticatedPrincipal principal = remoteAuthClient.authenticateAdmin(apiKey);
            request.setAttribute(PRINCIPAL_ATTR, principal);
            return principal;
        } catch (UnauthorizedException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

    public AuthenticatedPrincipal requirePermission(HttpServletRequest request, Permission permission) {
        AuthenticatedPrincipal principal = requireAdmin(request);
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
}
