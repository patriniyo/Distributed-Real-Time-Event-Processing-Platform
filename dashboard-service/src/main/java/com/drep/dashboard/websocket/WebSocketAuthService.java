package com.drep.dashboard.websocket;

import com.drep.common.security.Permission;
import com.drep.dashboard.auth.RemoteAuthClient;
import com.drep.dashboard.auth.UnauthorizedAuthException;
import com.drep.dashboard.config.DashboardProperties;
import org.springframework.stereotype.Service;

@Service
public class WebSocketAuthService {

    private final RemoteAuthClient remoteAuthClient;
    private final DashboardProperties properties;

    public WebSocketAuthService(RemoteAuthClient remoteAuthClient, DashboardProperties properties) {
        this.remoteAuthClient = remoteAuthClient;
        this.properties = properties;
    }

    public boolean isValidToken(String token) {
        try {
            var principal = remoteAuthClient.validate(token);
            principal.requirePermission(Permission.READ_EVENTS);
            return true;
        } catch (UnauthorizedAuthException | com.drep.common.security.AccessDeniedException ex) {
            return false;
        }
    }
}
