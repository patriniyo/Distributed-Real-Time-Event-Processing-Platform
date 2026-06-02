package com.drep.analytics.repository;

import com.drep.analytics.domain.AggregationMetricEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AggregationMetricRepository extends JpaRepository<AggregationMetricEntity, AggregationMetricEntity.AggregationMetricKey> {

    @Query("""
            SELECT a FROM AggregationMetricEntity a
            WHERE a.tenantId = :tenantId
              AND (:eventType IS NULL OR a.eventType = :eventType)
              AND a.windowSize = :windowSize
              AND a.bucketStart >= :from
              AND a.bucketStart < :to
            ORDER BY a.bucketStart ASC
            """)
    Page<AggregationMetricEntity> findMetrics(
            @Param("tenantId") String tenantId,
            @Param("eventType") String eventType,
            @Param("windowSize") String windowSize,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
            SELECT a FROM AggregationMetricEntity a
            WHERE a.tenantId = :tenantId
              AND a.windowSize = :windowSize
              AND a.bucketStart >= :from
              AND a.bucketStart < :to
            ORDER BY a.eventType, a.bucketStart ASC
            """)
    List<AggregationMetricEntity> findGroupedByEventType(
            @Param("tenantId") String tenantId,
            @Param("windowSize") String windowSize,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
