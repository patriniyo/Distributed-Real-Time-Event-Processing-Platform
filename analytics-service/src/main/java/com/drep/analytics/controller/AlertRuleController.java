package com.drep.analytics.controller;

import com.drep.analytics.config.AnalyticsProperties;
import com.drep.analytics.domain.AlertRuleEntity;
import com.drep.analytics.dto.AlertRuleRequest;
import com.drep.analytics.repository.AlertRuleRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/alert-rules")
public class AlertRuleController {

    private final AlertRuleRepository alertRuleRepository;
    private final AnalyticsProperties properties;

    public AlertRuleController(AlertRuleRepository alertRuleRepository,
                               AnalyticsProperties properties) {
        this.alertRuleRepository = alertRuleRepository;
        this.properties = properties;
    }

    @GetMapping
    public List<AlertRuleEntity> list(@RequestParam String tenantId, HttpServletRequest request) {
        requireAdmin(request);
        return alertRuleRepository.findByTenantIdAndEnabledTrue(tenantId);
    }

    @PostMapping
    public ResponseEntity<AlertRuleEntity> create(@Valid @RequestBody AlertRuleRequest body,
                                                  HttpServletRequest request) {
        requireAdmin(request);
        AlertRuleEntity rule = new AlertRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setTenantId(body.tenantId());
        rule.setEventType(body.eventType());
        rule.setMetric(body.metric());
        rule.setOperator(body.operator());
        rule.setThreshold(body.threshold());
        rule.setWebhookUrl(body.webhookUrl());
        rule.setEnabled(body.enabled());
        rule.setCreatedAt(Instant.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(alertRuleRepository.save(rule));
    }

    private void requireAdmin(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || !apiKey.equals(properties.getAdmin().getApiKey())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin API key required");
        }
    }
}
