package com.drep.analytics.controller;

import com.drep.analytics.config.AnalyticsProperties;
import com.drep.analytics.dto.AnalyticsQueryResponse;
import com.drep.analytics.service.AnalyticsQueryService;
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
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService queryService;
    private final AnalyticsProperties properties;

    public AnalyticsController(AnalyticsQueryService queryService, AnalyticsProperties properties) {
        this.queryService = queryService;
        this.properties = properties;
    }

    @GetMapping
    public AnalyticsQueryResponse query(
            @RequestParam String tenant,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "1m") String window,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String groupBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            HttpServletRequest request) {
        requireAuth(request);

        Instant rangeFrom = from != null ? from : Instant.now().minus(1, ChronoUnit.HOURS);
        Instant rangeTo = to != null ? to : Instant.now();

        return queryService.query(tenant, eventType, window, rangeFrom, rangeTo, groupBy, page, size);
    }

    private void requireAuth(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || !apiKey.equals(properties.getAdmin().getApiKey())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key required");
        }
    }
}
