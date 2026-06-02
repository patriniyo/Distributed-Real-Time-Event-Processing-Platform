package com.drep.analytics.service;

import com.drep.analytics.domain.AlertHistoryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class WebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifier.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String notify(String webhookUrl, AlertHistoryEntity alert) {
        try {
            String body = """
                    {"tenantId":"%s","eventType":"%s","metric":"%s","value":%s,"threshold":%s,"message":"%s"}
                    """.formatted(
                    alert.getTenantId(),
                    alert.getEventType() != null ? alert.getEventType() : "",
                    alert.getMetric(),
                    alert.getMetricValue(),
                    alert.getThreshold(),
                    alert.getMessage() != null ? alert.getMessage().replace("\"", "'") : ""
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300 ? "DELIVERED" : "FAILED";
        } catch (Exception e) {
            log.warn("Webhook delivery failed url={}: {}", webhookUrl, e.getMessage());
            return "FAILED";
        }
    }
}
