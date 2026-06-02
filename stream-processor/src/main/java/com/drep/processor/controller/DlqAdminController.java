package com.drep.processor.controller;

import com.drep.common.model.DlqEvent;
import com.drep.common.security.Permission;
import com.drep.processor.auth.ProcessorSecuritySupport;
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
    private final ProcessorSecuritySupport securitySupport;

    public DlqAdminController(DlqInspectionService dlqInspectionService,
                              DlqReplayService dlqReplayService,
                              ProcessorSecuritySupport securitySupport) {
        this.dlqInspectionService = dlqInspectionService;
        this.dlqReplayService = dlqReplayService;
        this.securitySupport = securitySupport;
    }

    @GetMapping
    public List<DlqEvent> list(@RequestParam(defaultValue = "100") int limit,
                               HttpServletRequest request) {
        securitySupport.requirePermission(request, Permission.ADMIN_DLQ);
        return dlqInspectionService.inspect(limit);
    }

    @GetMapping("/{dlqId}")
    public DlqEvent getById(@PathVariable UUID dlqId, HttpServletRequest request) {
        securitySupport.requirePermission(request, Permission.ADMIN_DLQ);
        return dlqInspectionService.inspectByDlqId(dlqId).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DLQ event not found"));
    }

    @PostMapping("/replay")
    public Map<String, Object> replayAll(@RequestParam(defaultValue = "100") int limit,
                                         HttpServletRequest request) {
        securitySupport.requirePermission(request, Permission.ADMIN_REPLAY);
        int replayed = dlqReplayService.replayAll(limit);
        return Map.of("replayed", replayed);
    }

    @PostMapping("/{dlqId}/replay")
    public Map<String, Object> replayOne(@PathVariable UUID dlqId, HttpServletRequest request) {
        securitySupport.requirePermission(request, Permission.ADMIN_REPLAY);
        boolean replayed = dlqReplayService.replayByDlqId(dlqId);
        if (!replayed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DLQ event not found");
        }
        return Map.of("replayed", true, "dlqId", dlqId);
    }
}
