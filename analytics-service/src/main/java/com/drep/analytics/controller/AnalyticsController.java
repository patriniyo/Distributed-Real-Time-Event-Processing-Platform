package com.drep.analytics.controller;

import com.drep.analytics.auth.SecuritySupport;
import com.drep.analytics.dto.AnalyticsQueryResponse;
import com.drep.analytics.service.AnalyticsQueryService;
import com.drep.common.security.Permission;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService queryService;
    private final SecuritySupport securitySupport;

    public AnalyticsController(AnalyticsQueryService queryService, SecuritySupport securitySupport) {
        this.queryService = queryService;
        this.securitySupport = securitySupport;
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
        securitySupport.requireTenantAccess(request, Permission.READ_ANALYTICS, tenant);

        Instant rangeFrom = from != null ? from : Instant.now().minus(1, ChronoUnit.HOURS);
        Instant rangeTo = to != null ? to : Instant.now();

        return queryService.query(tenant, eventType, window, rangeFrom, rangeTo, groupBy, page, size);
    }
}
