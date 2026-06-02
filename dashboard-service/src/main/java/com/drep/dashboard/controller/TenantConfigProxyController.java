package com.drep.dashboard.controller;

import com.drep.dashboard.client.ServiceClientFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/tenants")
@Tag(name = "Tenant Config", description = "Unified tenant configuration (retention + rate limits)")
public class TenantConfigProxyController {

    private final ServiceClientFactory clientFactory;
    private final ApiAuthHelper authHelper;

    public TenantConfigProxyController(ServiceClientFactory clientFactory, ApiAuthHelper authHelper) {
        this.clientFactory = clientFactory;
        this.authHelper = authHelper;
    }

    @GetMapping
    @Operation(summary = "List tenant configuration")
    public List<Map<String, Object>> list(HttpServletRequest request) {
        authHelper.requireAuth(request);
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();

        List<Map<String, Object>> retention = clientFactory.analyticsClient().get()
                .uri("/admin/tenants")
                .retrieve()
                .body(List.class);
        if (retention != null) {
            for (Map<String, Object> row : retention) {
                String tenantId = String.valueOf(row.get("tenantId"));
                merged.computeIfAbsent(tenantId, id -> baseTenant(id)).put("retentionDays", row.get("retentionDays"));
            }
        }

        List<Map<String, Object>> rateLimits = clientFactory.ingestionClient().get()
                .uri("/admin/tenants")
                .retrieve()
                .body(List.class);
        if (rateLimits != null) {
            for (Map<String, Object> row : rateLimits) {
                String tenantId = String.valueOf(row.get("tenantId"));
                merged.computeIfAbsent(tenantId, id -> baseTenant(id)).put("rateLimitRps", row.get("rateLimitRps"));
            }
        }

        return new ArrayList<>(merged.values());
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "Get tenant configuration")
    public Map<String, Object> get(@PathVariable String tenantId, HttpServletRequest request) {
        authHelper.requireAuth(request);
        Map<String, Object> config = baseTenant(tenantId);

        Map<?, ?> retention = clientFactory.analyticsClient().get()
                .uri("/admin/tenants/{tenantId}", tenantId)
                .retrieve()
                .body(Map.class);
        if (retention != null) {
            config.put("retentionDays", retention.get("retentionDays"));
        }

        Map<?, ?> rateLimit = clientFactory.ingestionClient().get()
                .uri("/admin/tenants/{tenantId}", tenantId)
                .retrieve()
                .body(Map.class);
        if (rateLimit != null) {
            config.put("rateLimitRps", rateLimit.get("rateLimitRps"));
        }

        return config;
    }

    @PutMapping("/{tenantId}")
    @Operation(summary = "Update tenant configuration")
    public Map<String, Object> update(@PathVariable String tenantId,
                                      @RequestBody Map<String, Object> body,
                                      HttpServletRequest request) {
        authHelper.requireAuth(request);

        if (body.containsKey("retentionDays")) {
            clientFactory.analyticsClient().put()
                    .uri("/admin/tenants/{tenantId}", tenantId)
                    .body(Map.of("retentionDays", body.get("retentionDays")))
                    .retrieve()
                    .toBodilessEntity();
        }

        if (body.containsKey("rateLimitRps")) {
            clientFactory.ingestionClient().put()
                    .uri("/admin/tenants/{tenantId}", tenantId)
                    .body(Map.of("rateLimitRps", body.get("rateLimitRps")))
                    .retrieve()
                    .toBodilessEntity();
        }

        return get(tenantId, request);
    }

    private Map<String, Object> baseTenant(String tenantId) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("tenantId", tenantId);
        return config;
    }
}
