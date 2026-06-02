package com.drep.analytics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "analytics-service",
                "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/internal/alerts/webhook-test")
    public Map<String, Object> webhookTest(@RequestBody Map<String, Object> payload) {
        return Map.of("received", true, "payload", payload, "timestamp", Instant.now().toString());
    }
}
