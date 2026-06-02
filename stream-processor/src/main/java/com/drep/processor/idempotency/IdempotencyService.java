package com.drep.processor.idempotency;

import java.util.UUID;

public interface IdempotencyService {
    /**
     * @return true if this eventId was acquired for processing (first time), false if duplicate
     */
    boolean tryAcquire(UUID eventId);
}
