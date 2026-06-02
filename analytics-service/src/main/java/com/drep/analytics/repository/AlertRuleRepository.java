package com.drep.analytics.repository;

import com.drep.analytics.domain.AlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, UUID> {

    List<AlertRuleEntity> findByTenantIdAndEnabledTrue(String tenantId);
}
