package com.drep.processor.controller;

import com.drep.common.model.DlqEvent;
import com.drep.processor.config.KafkaProperties;
import com.drep.processor.service.DlqInspectionService;
import com.drep.processor.service.DlqReplayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/dlq")
public class DlqAdminController {

    private final DlqInspectionService dlqInspectionService;
    private final DlqReplayService dlqReplayService;
    private final KafkaProperties kafkaProperties;

    public DlqAdminController(DlqInspectionService dlqInspectionService,
                              DlqReplayService dlqReplayService,
                              KafkaProperties kafkaProperties) {
        this.dlqInspectionService = dlqInspectionService;
        this.dlqReplayService = dlqReplayService;
        this.kafkaProperties = kafkaProperties;
    }

    @GetMapping
    public List<DlqEvent> list(@RequestParam(defaultValue = "100") int limit,
                               HttpServletRequest request) {
        requireAdmin(request);
        return dlqInspectionService.inspect(limit);
    }

    @GetMapping("/{dlqId}")
    public DlqEvent getById(@PathVariable UUID dlqId, HttpServletRequest request) {
        requireAdmin(request);
        return dlqInspectionService.inspectByDlqId(dlqId).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DLQ event not found"));
    }

    @PostMapping("/replay")
    public Map<String, Object> replayAll(@RequestParam(defaultValue = "100") int limit,
                                         HttpServletRequest request) {
        requireAdmin(request);
        int replayed = dlqReplayService.replayAll(limit);
        return Map.of("replayed", replayed);
    }

    @PostMapping("/{dlqId}/replay")
    public Map<String, Object> replayOne(@PathVariable UUID dlqId, HttpServletRequest request) {
        requireAdmin(request);
        boolean replayed = dlqReplayService.replayByDlqId(dlqId);
        if (!replayed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DLQ event not found");
        }
        return Map.of("replayed", true, "dlqId", dlqId);
    }

    private void requireAdmin(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || !apiKey.equals(kafkaProperties.getAdmin().getApiKey())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin API key required");
        }
    }
}
