package com.drep.analytics.controller;

import com.drep.analytics.config.AnalyticsProperties;
import com.drep.analytics.domain.TenantRetentionEntity;
import com.drep.analytics.dto.TenantRetentionDto;
import com.drep.analytics.dto.TenantRetentionUpdateRequest;
import com.drep.analytics.repository.TenantRetentionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin/tenants")
public class TenantConfigController {

    private final TenantRetentionRepository tenantRetentionRepository;
    private final AnalyticsProperties properties;

    public TenantConfigController(TenantRetentionRepository tenantRetentionRepository,
                                  AnalyticsProperties properties) {
        this.tenantRetentionRepository = tenantRetentionRepository;
        this.properties = properties;
    }

    @GetMapping
    public List<TenantRetentionDto> list(HttpServletRequest request) {
        requireAdmin(request);
        return tenantRetentionRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{tenantId}")
    public TenantRetentionDto get(@PathVariable String tenantId, HttpServletRequest request) {
        requireAdmin(request);
        return tenantRetentionRepository.findById(tenantId)
                .map(this::toDto)
                .orElseGet(() -> new TenantRetentionDto(tenantId, properties.getRetention().getDefaultDays()));
    }

    @PutMapping("/{tenantId}")
    public TenantRetentionDto update(@PathVariable String tenantId,
                                     @Valid @RequestBody TenantRetentionUpdateRequest body,
                                     HttpServletRequest request) {
        requireAdmin(request);
        TenantRetentionEntity entity = tenantRetentionRepository.findById(tenantId)
                .orElseGet(() -> {
                    TenantRetentionEntity created = new TenantRetentionEntity();
                    created.setTenantId(tenantId);
                    return created;
                });
        entity.setRetentionDays(body.retentionDays());
        tenantRetentionRepository.save(entity);
        properties.getTenantRetentionDays().put(tenantId, body.retentionDays());
        return toDto(entity);
    }

    private TenantRetentionDto toDto(TenantRetentionEntity entity) {
        return new TenantRetentionDto(entity.getTenantId(), entity.getRetentionDays());
    }

    private void requireAdmin(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || !apiKey.equals(properties.getAdmin().getApiKey())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin API key required");
        }
    }
}
