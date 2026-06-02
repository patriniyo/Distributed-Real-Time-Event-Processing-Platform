package com.drep.dashboard.websocket;

import com.drep.common.model.ProcessedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveEventWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveEventWebSocketHandler.class);

    private final WebSocketSessionManager sessionManager;
    private final WebSocketAuthService authService;
    private final ObjectMapper objectMapper;

    public LiveEventWebSocketHandler(WebSocketSessionManager sessionManager,
                                       WebSocketAuthService authService,
                                       ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Subscription subscription = parseSubscription(session);
        if (!authService.isValidToken(subscription.token())) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            return;
        }
        if (!sessionManager.register(session, subscription)) {
            session.close(CloseStatus.SERVICE_OVERLOAD.withReason("Max connections reached"));
            return;
        }
        log.info("WebSocket connected sessionId={} tenant={} eventType={}",
                session.getId(), subscription.tenantId(), subscription.eventType());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.unregister(session);
        log.debug("WebSocket closed sessionId={} status={}", session.getId(), status);
    }

    public void broadcast(ProcessedEvent event) {
        sessionManager.broadcast(event, objectMapper);
    }

    private Subscription parseSubscription(WebSocketSession session) {
        URI uri = session.getUri();
        Map<String, String> params = new ConcurrentHashMap<>();
        if (uri != null && uri.getQuery() != null) {
            for (String pair : uri.getQuery().split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    params.put(kv[0], java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
        return new Subscription(
                params.getOrDefault("tenant", ""),
                params.get("eventType"),
                params.get("token")
        );
    }

    record Subscription(String tenantId, String eventType, String token) {
    }
}
