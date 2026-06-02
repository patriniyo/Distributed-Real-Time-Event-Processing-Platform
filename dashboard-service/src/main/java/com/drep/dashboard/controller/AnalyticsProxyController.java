package com.drep.dashboard.controller;

import com.drep.common.security.Permission;
import com.drep.dashboard.client.ProxyUriBuilder;
import com.drep.dashboard.client.ServiceClientFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Query aggregated metrics (proxied to analytics-service)")
public class AnalyticsProxyController {

    private final ServiceClientFactory clientFactory;
    private final ApiAuthHelper authHelper;

    public AnalyticsProxyController(ServiceClientFactory clientFactory, ApiAuthHelper authHelper) {
        this.clientFactory = clientFactory;
        this.authHelper = authHelper;
    }

    @GetMapping
    @Operation(summary = "Query analytics metrics")
    public Map<?, ?> query(
            @RequestParam String tenant,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "1m") String window,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String groupBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            HttpServletRequest request) {
        authHelper.requireTenantAccess(request, Permission.READ_ANALYTICS, tenant);

        String uri = ProxyUriBuilder.build("/api/v1/analytics", ProxyUriBuilder.params(
                "tenant", tenant,
                "eventType", eventType,
                "window", window,
                "from", from,
                "to", to,
                "groupBy", groupBy,
                "page", page,
                "size", size
        ));

        return clientFactory.analyticsClient().get()
                .uri(uri)
                .retrieve()
                .body(Map.class);
    }
}
