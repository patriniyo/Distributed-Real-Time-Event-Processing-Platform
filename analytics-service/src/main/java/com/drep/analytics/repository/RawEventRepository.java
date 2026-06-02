package com.drep.analytics.repository;

import com.drep.analytics.domain.RawEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface RawEventRepository extends JpaRepository<RawEventEntity, UUID> {

    @Modifying
    @Query("DELETE FROM RawEventEntity r WHERE r.tenantId = :tenantId AND r.eventTimestamp < :cutoff")
    int deleteOlderThan(@Param("tenantId") String tenantId, @Param("cutoff") Instant cutoff);

    @Query("""
            SELECT r FROM RawEventEntity r
            WHERE r.tenantId = :tenantId
              AND (:eventType IS NULL OR r.eventType = :eventType)
              AND r.eventTimestamp >= :from
              AND r.eventTimestamp < :to
            ORDER BY r.eventTimestamp DESC
            """)
    org.springframework.data.domain.Page<RawEventEntity> findEvents(
            @Param("tenantId") String tenantId,
            @Param("eventType") String eventType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            org.springframework.data.domain.Pageable pageable);
}
