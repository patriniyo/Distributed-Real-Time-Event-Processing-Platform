package com.drep.analytics.controller;

import com.drep.analytics.config.AnalyticsProperties;
import com.drep.analytics.dto.EventQueryResponse;
import com.drep.analytics.service.EventQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1/events")
public class EventQueryController {

    private final EventQueryService eventQueryService;
    private final AnalyticsProperties properties;

    public EventQueryController(EventQueryService eventQueryService, AnalyticsProperties properties) {
        this.eventQueryService = eventQueryService;
        this.properties = properties;
    }

    @GetMapping
    public EventQueryResponse query(
            @RequestParam String tenant,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        requireAuth(request);
        return eventQueryService.query(tenant, eventType, from, to, page, size);
    }

    private void requireAuth(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || !apiKey.equals(properties.getAdmin().getApiKey())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key required");
        }
    }
}
