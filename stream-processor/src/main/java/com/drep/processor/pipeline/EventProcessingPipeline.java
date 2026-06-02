package com.drep.processor.pipeline;

import com.drep.common.model.Event;
import com.drep.common.model.ProcessedEvent;
import com.drep.processor.config.ProcessorProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventProcessingPipeline {

    private final List<EventTransformer> transformers;
    private final ProcessorProperties processorProperties;
    private final ObjectMapper objectMapper;

    public EventProcessingPipeline(NormalizationTransformer normalizationTransformer,
                                   EnrichmentTransformer enrichmentTransformer,
                                   FilterTransformer filterTransformer,
                                   ProcessorProperties processorProperties,
                                   ObjectMapper objectMapper) {
        this.transformers = List.of(normalizationTransformer, enrichmentTransformer, filterTransformer);
        this.processorProperties = processorProperties;
        this.objectMapper = objectMapper;
    }

    public Optional<ProcessedEvent> process(Event event) {
        Optional<Event> current = Optional.of(event);
        for (EventTransformer transformer : transformers) {
            if (current.isEmpty()) {
                return Optional.empty();
            }
            current = transformer.transform(current.get());
        }
        if (current.isEmpty()) {
            return Optional.empty();
        }
        Event enriched = current.get();
        JsonNode enrichment = enriched.payload() != null && enriched.payload().has("_enrichment")
                ? enriched.payload().get("_enrichment")
                : objectMapper.createObjectNode();
        return Optional.of(ProcessedEvent.from(
                stripEnrichmentFromPayload(enriched),
                processorProperties.getSchemaVersion(),
                enrichment
        ));
    }

    private Event stripEnrichmentFromPayload(Event event) {
        if (event.payload() == null || !event.payload().isObject()) {
            return event;
        }
        var payloadCopy = ((com.fasterxml.jackson.databind.node.ObjectNode) event.payload().deepCopy());
        payloadCopy.remove("_enrichment");
        return new Event(
                event.eventId(),
                event.eventType(),
                event.timestamp(),
                event.tenantId(),
                payloadCopy,
                event.traceId(),
                event.ingestedAt()
        );
    }
}
