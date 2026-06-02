package com.drep.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_history")
public class AlertHistoryEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID ruleId;

    @Column(nullable = false)
    private String tenantId;

    private String eventType;

    @Column(nullable = false)
    private String metric;

    @Column(nullable = false)
    private double metricValue;

    @Column(nullable = false)
    private double threshold;

    @Column(nullable = false)
    private Instant firedAt;

    private String webhookStatus;
    private String message;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public void setRuleId(UUID ruleId) {
        this.ruleId = ruleId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(double metricValue) {
        this.metricValue = metricValue;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public Instant getFiredAt() {
        return firedAt;
    }

    public void setFiredAt(Instant firedAt) {
        this.firedAt = firedAt;
    }

    public String getWebhookStatus() {
        return webhookStatus;
    }

    public void setWebhookStatus(String webhookStatus) {
        this.webhookStatus = webhookStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
