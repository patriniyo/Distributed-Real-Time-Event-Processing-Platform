package com.drep.analytics.controller;

import com.drep.analytics.auth.AuditService;
import com.drep.analytics.auth.SecuritySupport;
import com.drep.analytics.dto.EventQueryResponse;
import com.drep.analytics.service.EventQueryService;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.Permission;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/events")
public class EventQueryController {

    private final EventQueryService eventQueryService;
    private final SecuritySupport securitySupport;

    public EventQueryController(EventQueryService eventQueryService, SecuritySupport securitySupport) {
        this.eventQueryService = eventQueryService;
        this.securitySupport = securitySupport;
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
        securitySupport.requireTenantAccess(request, Permission.READ_EVENTS, tenant);
        return eventQueryService.query(tenant, eventType, from, to, page, size);
    }
}
