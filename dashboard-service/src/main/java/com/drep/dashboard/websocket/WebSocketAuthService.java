package com.drep.dashboard.websocket;

import com.drep.dashboard.config.DashboardProperties;
import org.springframework.stereotype.Service;

@Service
public class WebSocketAuthService {

    private final DashboardProperties properties;

    public WebSocketAuthService(DashboardProperties properties) {
        this.properties = properties;
    }

    public boolean isValidToken(String token) {
        return token != null && token.equals(properties.getApiKey());
    }
}
