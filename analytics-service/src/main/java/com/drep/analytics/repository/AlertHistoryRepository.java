package com.drep.analytics.repository;

import com.drep.analytics.domain.AlertHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertHistoryRepository extends JpaRepository<AlertHistoryEntity, UUID> {
}
