package com.drep.dashboard.controller;

import com.drep.dashboard.websocket.WebSocketSessionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private final WebSocketSessionManager sessionManager;

    public HealthController(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "dashboard-service",
                "activeWebSocketConnections", sessionManager.activeConnections(),
                "timestamp", Instant.now().toString()
        ));
    }
}
