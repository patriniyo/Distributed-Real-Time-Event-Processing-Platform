package com.drep.ingestion.controller;

import com.drep.ingestion.config.IngestionProperties;
import com.drep.ingestion.dto.TenantRateLimitDto;
import com.drep.ingestion.dto.TenantRateLimitUpdateRequest;
import com.drep.ingestion.service.TenantRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/tenants")
public class TenantAdminController {

    private final IngestionProperties properties;
    private final TenantRateLimitService rateLimitService;

    public TenantAdminController(IngestionProperties properties, TenantRateLimitService rateLimitService) {
        this.properties = properties;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public List<TenantRateLimitDto> list(HttpServletRequest request) {
        requireAdmin(request);
        List<TenantRateLimitDto> tenants = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : properties.getTenantRateLimits().entrySet()) {
            tenants.add(new TenantRateLimitDto(entry.getKey(), entry.getValue()));
        }
        return tenants;
    }

    @GetMapping("/{tenantId}")
    public TenantRateLimitDto get(@PathVariable String tenantId, HttpServletRequest request) {
        requireAdmin(request);
        return new TenantRateLimitDto(tenantId, rateLimitService.getLimitForTenant(tenantId));
    }

    @PutMapping("/{tenantId}")
    public TenantRateLimitDto update(@PathVariable String tenantId,
                                     @Valid @RequestBody TenantRateLimitUpdateRequest body,
                                     HttpServletRequest request) {
        requireAdmin(request);
        properties.getTenantRateLimits().put(tenantId, body.rateLimitRps());
        return new TenantRateLimitDto(tenantId, body.rateLimitRps());
    }

    private void requireAdmin(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || !apiKey.equals(properties.getAdmin().getApiKey())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin API key required");
        }
    }
}
