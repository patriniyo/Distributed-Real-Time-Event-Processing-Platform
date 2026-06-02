package com.drep.dashboard.websocket;

import com.drep.common.model.ProcessedEvent;
import com.drep.dashboard.config.DashboardProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketSessionManagerTest {

    private WebSocketSessionManager manager;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        DashboardProperties properties = new DashboardProperties();
        manager = new WebSocketSessionManager(properties);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void broadcastsToMatchingTenantOnly() throws Exception {
        FakeWebSocketSession sessionA = new FakeWebSocketSession("s1");
        FakeWebSocketSession sessionB = new FakeWebSocketSession("s2");

        manager.register(sessionA, new LiveEventWebSocketHandler.Subscription("tenant-a", null, "token"));
        manager.register(sessionB, new LiveEventWebSocketHandler.Subscription("tenant-b", null, "token"));

        ProcessedEvent event = new ProcessedEvent(
                UUID.randomUUID(),
                "page.view",
                Instant.now(),
                "tenant-a",
                JsonNodeFactory.instance.objectNode().put("url", "/home"),
                "trace-1",
                Instant.now(),
                Instant.now(),
                1,
                JsonNodeFactory.instance.objectNode()
        );

        manager.broadcast(event, objectMapper);

        assertEquals(1, sessionA.sentMessages().size());
        assertTrue(sessionB.sentMessages().isEmpty());
    }
}
