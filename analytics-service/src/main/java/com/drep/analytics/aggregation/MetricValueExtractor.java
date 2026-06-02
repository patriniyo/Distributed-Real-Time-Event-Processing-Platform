package com.drep.analytics.aggregation;

import com.drep.common.model.ProcessedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class MetricValueExtractor {

    private static final String[] NUMERIC_FIELDS = {"amount", "value", "score", "price", "quantity"};

    public Optional<Double> extract(ProcessedEvent event) {
        JsonNode payload = event.payload();
        if (payload == null) {
            return Optional.empty();
        }
        for (String field : NUMERIC_FIELDS) {
            if (payload.has(field) && payload.get(field).isNumber()) {
                return Optional.of(payload.get(field).asDouble());
            }
        }
        return Optional.empty();
    }

    public static Instant bucketStart(Instant eventTime, WindowSize window) {
        long epochMilli = eventTime.toEpochMilli();
        long windowMillis = window.duration().toMillis();
        return Instant.ofEpochMilli(epochMilli - Math.floorMod(epochMilli, windowMillis));
    }

    public static Instant bucketEnd(Instant bucketStart, WindowSize window) {
        return bucketStart.plus(window.duration());
    }
}
