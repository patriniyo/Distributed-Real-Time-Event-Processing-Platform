package com.drep.analytics.repository;

import com.drep.analytics.domain.AuditLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    List<AuditLogEntity> findByOrderByOccurredAtDesc(Pageable pageable);
}
