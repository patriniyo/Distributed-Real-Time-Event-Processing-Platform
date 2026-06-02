package com.drep.processor.auth;

import com.drep.common.security.AccessDeniedException;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.Permission;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProcessorSecuritySupport {

    private final RemoteAuthClient remoteAuthClient;

    public ProcessorSecuritySupport(RemoteAuthClient remoteAuthClient) {
        this.remoteAuthClient = remoteAuthClient;
    }

    public AuthenticatedPrincipal requirePermission(HttpServletRequest request, Permission permission) {
        AuthenticatedPrincipal principal = remoteAuthClient.requireAdmin(request.getHeader("X-API-Key"));
        try {
            principal.requirePermission(permission);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        }
        return principal;
    }
}
