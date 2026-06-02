package com.drep.processor.idempotency;

import com.drep.processor.config.ProcessorProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "drep.processor.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisIdempotencyService implements IdempotencyService {

    private static final String KEY_PREFIX = "idempotency:event:";

    private final StringRedisTemplate redisTemplate;
    private final ProcessorProperties processorProperties;

    public RedisIdempotencyService(StringRedisTemplate redisTemplate,
                                   ProcessorProperties processorProperties) {
        this.redisTemplate = redisTemplate;
        this.processorProperties = processorProperties;
    }

    @Override
    public boolean tryAcquire(UUID eventId) {
        Duration ttl = Duration.ofHours(processorProperties.getIdempotency().getTtlHours());
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + eventId, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }
}
