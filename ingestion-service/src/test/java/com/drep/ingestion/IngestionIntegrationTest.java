package com.drep.ingestion;

import com.drep.ingestion.auth.JwtService;
import com.drep.ingestion.service.EventIngestionService;
import com.drep.ingestion.service.TenantRateLimitService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventIngestionService ingestionService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TenantRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        ingestionService.clearProcessedEvents();
        rateLimitService.reset();
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void ingestSingleEventReturns202() throws Exception {
        String body = """
                {
                  "eventType": "page.view",
                  "timestamp": "%s",
                  "tenantId": "tenant-a",
                  "payload": {"page": "/home"}
                }
                """.formatted(Instant.now());

        mockMvc.perform(post("/api/v1/events")
                        .header("X-API-Key", "tenant-a-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedCount").value(1))
                .andExpect(jsonPath("$.eventIds", hasSize(1)));
    }

    @Test
    void ingestBatchEventsReturns202() throws Exception {
        String body = """
                {
                  "events": [
                    {
                      "eventType": "click",
                      "timestamp": "%s",
                      "tenantId": "tenant-a",
                      "payload": {"button": "signup"}
                    },
                    {
                      "eventType": "click",
                      "timestamp": "%s",
                      "tenantId": "tenant-a",
                      "payload": {"button": "login"}
                    }
                  ]
                }
                """.formatted(Instant.now(), Instant.now());

        mockMvc.perform(post("/api/v1/events")
                        .header("X-API-Key", "tenant-a-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.acceptedCount").value(2));
    }

    @Test
    void invalidPayloadReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("X-API-Key", "tenant-a-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void missingAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void jwtAuthWorks() throws Exception {
        String token = jwtService.generateToken("tenant-a", 60_000);
        String body = """
                {
                  "eventType": "order.created",
                  "timestamp": "%s",
                  "tenantId": "tenant-a",
                  "payload": {"orderId": "123"}
                }
                """.formatted(Instant.now());

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());
    }

    @Test
    void tenantMismatchReturns401() throws Exception {
        String body = """
                {
                  "eventType": "page.view",
                  "timestamp": "%s",
                  "tenantId": "tenant-b",
                  "payload": {}
                }
                """.formatted(Instant.now());

        mockMvc.perform(post("/api/v1/events")
                        .header("X-API-Key", "tenant-a-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitReturns429() throws Exception {
        String body = """
                {
                  "eventType": "page.view",
                  "timestamp": "%s",
                  "tenantId": "tenant-a",
                  "payload": {}
                }
                """.formatted(Instant.now());

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/events")
                            .header("X-API-Key", "tenant-a-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isAccepted());
        }

        mockMvc.perform(post("/api/v1/events")
                        .header("X-API-Key", "tenant-a-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"));
    }
}
