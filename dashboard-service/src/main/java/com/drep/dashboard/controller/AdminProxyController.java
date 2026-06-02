package com.drep.dashboard.controller;

import com.drep.common.security.Permission;
import com.drep.dashboard.client.ServiceClientFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "DLQ management and replay (proxied to stream-processor)")
public class AdminProxyController {

    private final ServiceClientFactory clientFactory;
    private final ApiAuthHelper authHelper;

    public AdminProxyController(ServiceClientFactory clientFactory, ApiAuthHelper authHelper) {
        this.clientFactory = clientFactory;
        this.authHelper = authHelper;
    }

    @GetMapping("/dlq")
    @Operation(summary = "List DLQ events")
    public List<?> listDlq(@RequestParam(defaultValue = "100") int limit, HttpServletRequest request) {
        authHelper.requirePermission(request, Permission.ADMIN_DLQ);
        return clientFactory.streamProcessorClient().get()
                .uri("/admin/dlq?limit={limit}", limit)
                .retrieve()
                .body(List.class);
    }

    @PostMapping("/dlq/replay")
    @Operation(summary = "Replay all DLQ events")
    public Map<?, ?> replayAll(@RequestParam(defaultValue = "100") int limit, HttpServletRequest request) {
        authHelper.requirePermission(request, Permission.ADMIN_REPLAY);
        return clientFactory.streamProcessorClient().post()
                .uri("/admin/dlq/replay?limit={limit}", limit)
                .retrieve()
                .body(Map.class);
    }

    @PostMapping("/dlq/{dlqId}/replay")
    @Operation(summary = "Replay a single DLQ event")
    public Map<?, ?> replayOne(@PathVariable UUID dlqId, HttpServletRequest request) {
        authHelper.requirePermission(request, Permission.ADMIN_REPLAY);
        return clientFactory.streamProcessorClient().post()
                .uri("/admin/dlq/{dlqId}/replay", dlqId)
                .retrieve()
                .body(Map.class);
    }

    @PostMapping("/replay")
    @Operation(summary = "Replay events from Kafka by time range")
    public Map<?, ?> replay(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        authHelper.requirePermission(request, Permission.ADMIN_REPLAY);
        return clientFactory.streamProcessorClient().post()
                .uri("/admin/replay")
                .body(body)
                .retrieve()
                .body(Map.class);
    }

    @GetMapping("/replay/{jobId}")
    @Operation(summary = "Get replay job status")
    public Map<?, ?> replayStatus(@PathVariable UUID jobId, HttpServletRequest request) {
        authHelper.requirePermission(request, Permission.ADMIN_REPLAY);
        return clientFactory.streamProcessorClient().get()
                .uri("/admin/replay/{jobId}", jobId)
                .retrieve()
                .body(Map.class);
    }
}
