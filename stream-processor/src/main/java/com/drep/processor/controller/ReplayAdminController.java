package com.drep.processor.controller;

import com.drep.common.security.Permission;
import com.drep.processor.auth.ProcessorSecuritySupport;
import com.drep.processor.dto.ReplayJobResponse;
import com.drep.processor.dto.ReplayRequest;
import com.drep.processor.service.EventReplayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/admin/replay")
public class ReplayAdminController {

    private final EventReplayService eventReplayService;
    private final ProcessorSecuritySupport securitySupport;

    public ReplayAdminController(EventReplayService eventReplayService,
                                 ProcessorSecuritySupport securitySupport) {
        this.eventReplayService = eventReplayService;
        this.securitySupport = securitySupport;
    }

    @PostMapping
    public ResponseEntity<ReplayJobResponse> startReplay(@Valid @RequestBody ReplayRequest request,
                                                         HttpServletRequest httpRequest) {
        securitySupport.requirePermission(httpRequest, Permission.ADMIN_REPLAY);
        return ResponseEntity.accepted().body(eventReplayService.startReplay(request));
    }

    @GetMapping("/{jobId}")
    public ReplayJobResponse getReplayJob(@PathVariable UUID jobId, HttpServletRequest httpRequest) {
        securitySupport.requirePermission(httpRequest, Permission.ADMIN_REPLAY);
        ReplayJobResponse job = eventReplayService.getJob(jobId);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay job not found");
        }
        return job;
    }
}
