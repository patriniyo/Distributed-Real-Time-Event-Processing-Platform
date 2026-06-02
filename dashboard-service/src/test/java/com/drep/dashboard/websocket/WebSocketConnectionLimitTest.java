package com.drep.dashboard.websocket;

import com.drep.dashboard.config.DashboardProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketConnectionLimitTest {

    @Test
    void supportsUpToConfiguredMaxConnections() {
        DashboardProperties properties = new DashboardProperties();
        properties.getWebSocket().setMaxConnections(1000);
        WebSocketSessionManager manager = new WebSocketSessionManager(properties);

        for (int i = 0; i < 1000; i++) {
            FakeWebSocketSession session = new FakeWebSocketSession("session-" + i);
            assertTrue(manager.register(session,
                    new LiveEventWebSocketHandler.Subscription("tenant-a", null, "token")));
        }

        FakeWebSocketSession overflow = new FakeWebSocketSession("overflow");
        assertFalse(manager.register(overflow,
                new LiveEventWebSocketHandler.Subscription("tenant-a", null, "token")));
        assertTrue(manager.activeConnections() >= 1000);
    }
}
