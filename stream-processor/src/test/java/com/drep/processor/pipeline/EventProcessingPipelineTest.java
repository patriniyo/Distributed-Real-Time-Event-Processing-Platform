package com.drep.processor.pipeline;

import com.drep.common.model.Event;
import com.drep.common.model.ProcessedEvent;
import com.drep.processor.config.ProcessorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventProcessingPipelineTest {

    private EventProcessingPipeline pipeline;

    @BeforeEach
    void setUp() {
        ProcessorProperties properties = new ProcessorProperties();
        properties.setSchemaVersion(1);
        properties.getTenantTiers().put("tenant-a", "premium");

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        pipeline = new EventProcessingPipeline(
                new NormalizationTransformer(),
                new EnrichmentTransformer(properties, objectMapper),
                new FilterTransformer(properties),
                properties,
                objectMapper
        );
    }

    @Test
    void normalizesEventTypeAndEnrichesPayload() {
        Event event = new Event(
                UUID.randomUUID(),
                "  Page.View ",
                Instant.now(),
                "tenant-a",
                null,
                "trace-1",
                Instant.now()
        );

        Optional<ProcessedEvent> result = pipeline.process(event);

        assertThat(result).isPresent();
        assertThat(result.get().eventType()).isEqualTo("page.view");
        assertThat(result.get().enrichment().get("tenantTier").asText()).isEqualTo("premium");
        assertThat(result.get().schemaVersion()).isEqualTo(1);
    }

    @Test
    void filtersBlockedEventTypes() {
        ProcessorProperties properties = new ProcessorProperties();
        properties.getBlockedEventTypes().add("spam.click");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventProcessingPipeline filteringPipeline = new EventProcessingPipeline(
                new NormalizationTransformer(),
                new EnrichmentTransformer(properties, objectMapper),
                new FilterTransformer(properties),
                properties,
                objectMapper
        );

        Event event = new Event(
                UUID.randomUUID(),
                "spam.click",
                Instant.now(),
                "tenant-a",
                null,
                "trace-1",
                Instant.now()
        );

        assertThat(filteringPipeline.process(event)).isEmpty();
    }
}
