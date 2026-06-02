package com.drep.analytics.service;

import com.drep.analytics.domain.AggregationMetricEntity;
import com.drep.analytics.domain.AlertHistoryEntity;
import com.drep.analytics.domain.AlertRuleEntity;
import com.drep.analytics.repository.AlertHistoryRepository;
import com.drep.analytics.repository.AlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AlertRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleEngine.class);

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final WebhookNotifier webhookNotifier;

    public AlertRuleEngine(AlertRuleRepository alertRuleRepository,
                           AlertHistoryRepository alertHistoryRepository,
                           WebhookNotifier webhookNotifier) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertHistoryRepository = alertHistoryRepository;
        this.webhookNotifier = webhookNotifier;
    }

    @Transactional
    public void evaluate(AggregationMetricEntity metric) {
        List<AlertRuleEntity> rules = alertRuleRepository.findByTenantIdAndEnabledTrue(metric.getTenantId());
        for (AlertRuleEntity rule : rules) {
            if (rule.getEventType() != null && !rule.getEventType().equals(metric.getEventType())) {
                continue;
            }
            Double value = resolveMetricValue(metric, rule.getMetric());
            if (value == null) {
                continue;
            }
            if (breaches(value, rule.getThreshold(), rule.getOperator())) {
                fireAlert(rule, metric, value);
            }
        }
    }

    private Double resolveMetricValue(AggregationMetricEntity metric, String metricName) {
        return switch (metricName) {
            case "event_count", "count" -> (double) metric.getEventCount();
            case "sum" -> metric.getSumValue();
            case "avg" -> metric.getAvgValue();
            case "min" -> metric.getMinValue();
            case "max" -> metric.getMaxValue();
            case "p50" -> metric.getP50();
            case "p95" -> metric.getP95();
            case "p99" -> metric.getP99();
            default -> null;
        };
    }

    private boolean breaches(double value, double threshold, String operator) {
        return switch (operator) {
            case "gt" -> value > threshold;
            case "gte" -> value >= threshold;
            case "lt" -> value < threshold;
            case "lte" -> value <= threshold;
            default -> false;
        };
    }

    private void fireAlert(AlertRuleEntity rule, AggregationMetricEntity metric, double value) {
        AlertHistoryEntity history = new AlertHistoryEntity();
        history.setId(UUID.randomUUID());
        history.setRuleId(rule.getId());
        history.setTenantId(rule.getTenantId());
        history.setEventType(metric.getEventType());
        history.setMetric(rule.getMetric());
        history.setMetricValue(value);
        history.setThreshold(rule.getThreshold());
        history.setFiredAt(Instant.now());
        history.setMessage("Alert: " + rule.getMetric() + " " + rule.getOperator() + " " + rule.getThreshold()
                + " (actual=" + value + ")");

        if (rule.getWebhookUrl() != null && !rule.getWebhookUrl().isBlank()) {
            String status = webhookNotifier.notify(rule.getWebhookUrl(), history);
            history.setWebhookStatus(status);
        } else {
            history.setWebhookStatus("SKIPPED");
        }

        alertHistoryRepository.save(history);
        log.warn("Alert fired ruleId={} tenantId={} metric={} value={} threshold={}",
                rule.getId(), rule.getTenantId(), rule.getMetric(), value, rule.getThreshold());
    }
}
