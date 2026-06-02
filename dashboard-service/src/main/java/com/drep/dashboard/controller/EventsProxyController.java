package com.drep.dashboard.controller;

import com.drep.common.security.Permission;
import com.drep.dashboard.client.ProxyUriBuilder;
import com.drep.dashboard.client.ServiceClientFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Ingest and query events (proxied to ingestion/analytics services)")
public class EventsProxyController {

    private final ServiceClientFactory clientFactory;
    private final ApiAuthHelper authHelper;

    public EventsProxyController(ServiceClientFactory clientFactory, ApiAuthHelper authHelper) {
        this.clientFactory = clientFactory;
        this.authHelper = authHelper;
    }

    @PostMapping
    @Operation(summary = "Ingest a single event")
    public Map<?, ?> ingest(@RequestBody Map<String, Object> event, HttpServletRequest request) {
        authHelper.requirePermission(request, Permission.INGEST_EVENTS);
        return clientFactory.ingestionClient().post()
                .uri("/api/v1/events")
                .body(event)
                .retrieve()
                .body(Map.class);
    }

    @GetMapping
    @Operation(summary = "Query historical events")
    public Map<?, ?> query(
            @RequestParam String tenant,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        authHelper.requireTenantAccess(request, Permission.READ_EVENTS, tenant);

        String uri = ProxyUriBuilder.build("/api/v1/events", ProxyUriBuilder.params(
                "tenant", tenant,
                "eventType", eventType,
                "from", from,
                "to", to,
                "page", page,
                "size", size
        ));

        return clientFactory.analyticsClient().get()
                .uri(uri)
                .retrieve()
                .body(Map.class);
    }
}
