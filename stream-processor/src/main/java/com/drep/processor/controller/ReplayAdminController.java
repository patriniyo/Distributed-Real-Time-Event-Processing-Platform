package com.drep.processor.controller;

import com.drep.processor.config.KafkaProperties;
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
    private final KafkaProperties kafkaProperties;

    public ReplayAdminController(EventReplayService eventReplayService, KafkaProperties kafkaProperties) {
        this.eventReplayService = eventReplayService;
        this.kafkaProperties = kafkaProperties;
    }

    @PostMapping
    public ResponseEntity<ReplayJobResponse> startReplay(@Valid @RequestBody ReplayRequest request,
                                                         HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ResponseEntity.accepted().body(eventReplayService.startReplay(request));
    }

    @GetMapping("/{jobId}")
    public ReplayJobResponse getReplayJob(@PathVariable UUID jobId, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        ReplayJobResponse job = eventReplayService.getJob(jobId);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Replay job not found");
        }
        return job;
    }

    private void requireAdmin(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || !apiKey.equals(kafkaProperties.getAdmin().getApiKey())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin API key required");
        }
    }
}
