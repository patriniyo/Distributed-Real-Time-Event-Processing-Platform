package com.drep.processor.pipeline;

import com.drep.common.model.Event;
import com.drep.processor.config.ProcessorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EnrichmentTransformer implements EventTransformer {

    private final ProcessorProperties processorProperties;
    private final ObjectMapper objectMapper;

    public EnrichmentTransformer(ProcessorProperties processorProperties, ObjectMapper objectMapper) {
        this.processorProperties = processorProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Event> transform(Event event) {
        ObjectNode enrichment = objectMapper.createObjectNode();
        enrichment.put("schemaVersion", processorProperties.getSchemaVersion());
        enrichment.put("processedBy", "stream-processor");
        enrichment.put("tenantTier",
                processorProperties.getTenantTiers().getOrDefault(event.tenantId(), "standard"));
        enrichment.put("normalized", true);

        ObjectNode payload = event.payload() != null && event.payload().isObject()
                ? (ObjectNode) event.payload().deepCopy()
                : objectMapper.createObjectNode();
        payload.set("_enrichment", enrichment);

        return Optional.of(new Event(
                event.eventId(),
                event.eventType(),
                event.timestamp(),
                event.tenantId(),
                payload,
                event.traceId(),
                event.ingestedAt()
        ));
    }
}
