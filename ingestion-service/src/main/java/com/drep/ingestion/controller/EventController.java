package com.drep.ingestion.controller;

import com.drep.ingestion.filter.AuthFilter;
import com.drep.ingestion.dto.BatchEventRequest;
import com.drep.ingestion.dto.EventRequest;
import com.drep.ingestion.dto.IngestionResponse;
import com.drep.ingestion.exception.UnauthorizedException;
import com.drep.common.dto.ApiErrorResponse;
import com.drep.ingestion.filter.TraceIdFilter;
import com.drep.common.model.Event;
import com.drep.ingestion.service.EventIngestionService;
import com.drep.ingestion.service.TenantRateLimitService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
public class EventController {

    private final EventIngestionService ingestionService;
    private final TenantRateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public EventController(EventIngestionService ingestionService,
                           TenantRateLimitService rateLimitService,
                           ObjectMapper objectMapper,
                           Validator validator) {
        this.ingestionService = ingestionService;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping("/events")
    public ResponseEntity<IngestionResponse> ingestEvents(@RequestBody JsonNode body,
                                                          HttpServletRequest request) {
        String traceId = TraceIdFilter.getTraceId(request);
        String authenticatedTenant = AuthFilter.getAuthenticatedTenantId(request);

        List<EventRequest> events = parseEvents(body);
        validate(events);
        validateTenantAccess(events, authenticatedTenant);
        rateLimitService.checkRateLimit(authenticatedTenant, events.size());

        List<Event> accepted = ingestionService.acceptEvents(events, traceId);
        List<String> eventIds = accepted.stream().map(e -> e.eventId().toString()).toList();

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(IngestionResponse.accepted(traceId, accepted.size(), eventIds));
    }

    private List<EventRequest> parseEvents(JsonNode body) {
        if (body == null || body.isNull()) {
            throw new HttpMessageNotReadableException("Request body is required");
        }

        try {
            if (body.isArray()) {
                EventRequest[] array = objectMapper.treeToValue(body, EventRequest[].class);
                if (array.length == 0) {
                    throw new HttpMessageNotReadableException("events list must not be empty");
                }
                if (array.length > 1000) {
                    throw new HttpMessageNotReadableException("batch size must not exceed 1000 events");
                }
                return List.of(array);
            }

            if (body.has("events")) {
                BatchEventRequest batch = objectMapper.treeToValue(body, BatchEventRequest.class);
                return batch.events();
            }

            EventRequest single = objectMapper.treeToValue(body, EventRequest.class);
            return List.of(single);
        } catch (JsonProcessingException e) {
            throw new HttpMessageNotReadableException("Invalid JSON payload", e);
        }
    }

    private void validate(List<EventRequest> events) {
        List<ApiErrorResponse.FieldError> errors = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            Set<ConstraintViolation<EventRequest>> violations = validator.validate(events.get(i));
            int index = i;
            violations.forEach(v -> errors.add(
                    new ApiErrorResponse.FieldError(
                            events.size() == 1 ? v.getPropertyPath().toString()
                                    : "events[" + index + "]." + v.getPropertyPath(),
                            v.getMessage())));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void validateTenantAccess(List<EventRequest> events, String authenticatedTenant) {
        for (EventRequest event : events) {
            if (!authenticatedTenant.equals(event.tenantId())) {
                throw new UnauthorizedException(
                        "Tenant mismatch: authenticated as '" + authenticatedTenant
                                + "' but event has tenantId '" + event.tenantId() + "'");
            }
        }
    }
}
