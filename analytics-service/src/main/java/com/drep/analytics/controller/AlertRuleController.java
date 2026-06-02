package com.drep.analytics.controller;

import com.drep.analytics.auth.AuditService;
import com.drep.analytics.auth.SecuritySupport;
import com.drep.analytics.domain.AlertRuleEntity;
import com.drep.analytics.dto.AlertRuleRequest;
import com.drep.analytics.repository.AlertRuleRepository;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.Permission;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/alert-rules")
public class AlertRuleController {

    private final AlertRuleRepository alertRuleRepository;
    private final SecuritySupport securitySupport;
    private final AuditService auditService;

    public AlertRuleController(AlertRuleRepository alertRuleRepository,
                               SecuritySupport securitySupport,
                               AuditService auditService) {
        this.alertRuleRepository = alertRuleRepository;
        this.securitySupport = securitySupport;
        this.auditService = auditService;
    }

    @GetMapping
    public List<AlertRuleEntity> list(@RequestParam String tenantId, HttpServletRequest request) {
        securitySupport.requireTenantAccess(request, Permission.ADMIN_TENANT_CONFIG, tenantId);
        return alertRuleRepository.findByTenantIdAndEnabledTrue(tenantId);
    }

    @PostMapping
    public ResponseEntity<AlertRuleEntity> create(@Valid @RequestBody AlertRuleRequest body,
                                                  HttpServletRequest request) {
        AuthenticatedPrincipal actor = securitySupport.requireTenantAccess(
                request, Permission.ADMIN_TENANT_CONFIG, body.tenantId());
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
        AlertRuleEntity saved = alertRuleRepository.save(rule);
        auditService.log(actor, "ALERT_RULE_CREATE", "alert-rules/" + saved.getId(), "tenant=" + body.tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
