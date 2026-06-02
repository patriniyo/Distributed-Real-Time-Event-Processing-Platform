package com.drep.analytics.repository;

import com.drep.analytics.domain.TenantRetentionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRetentionRepository extends JpaRepository<TenantRetentionEntity, String> {
}
