package com.drep.processor.idempotency;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(prefix = "drep.processor.idempotency", name = "enabled", havingValue = "false")
public class InMemoryIdempotencyService implements IdempotencyService {

    private final Set<UUID> seen = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryAcquire(UUID eventId) {
        return seen.add(eventId);
    }

    public void clear() {
        seen.clear();
    }
}
