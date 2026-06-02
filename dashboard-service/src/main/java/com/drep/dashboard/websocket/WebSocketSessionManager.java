package com.drep.dashboard.websocket;

import com.drep.common.model.ProcessedEvent;
import com.drep.dashboard.config.DashboardProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);

    private final DashboardProperties properties;
    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();

    public WebSocketSessionManager(DashboardProperties properties) {
        this.properties = properties;
    }

    public boolean register(WebSocketSession session, LiveEventWebSocketHandler.Subscription subscription) {
        if (sessions.size() >= properties.getWebSocket().getMaxConnections()) {
            return false;
        }
        sessions.put(session.getId(), new SessionEntry(session, subscription));
        return true;
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    public int activeConnections() {
        return sessions.size();
    }

    public void broadcast(ProcessedEvent event, ObjectMapper objectMapper) {
        Iterator<Map.Entry<String, SessionEntry>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SessionEntry> entry = iterator.next();
            SessionEntry sessionEntry = entry.getValue();
            LiveEventWebSocketHandler.Subscription sub = sessionEntry.subscription();
            WebSocketSession session = sessionEntry.session();

            if (!session.isOpen()) {
                iterator.remove();
                continue;
            }
            if (!sub.tenantId().equals(event.tenantId())) {
                continue;
            }
            if (sub.eventType() != null && !sub.eventType().isBlank()
                    && !sub.eventType().equals(event.eventType())) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
            } catch (Exception e) {
                log.warn("Failed to send WebSocket message sessionId={}: {}", session.getId(), e.getMessage());
            }
        }
    }

    private record SessionEntry(WebSocketSession session, LiveEventWebSocketHandler.Subscription subscription) {
    }
}
