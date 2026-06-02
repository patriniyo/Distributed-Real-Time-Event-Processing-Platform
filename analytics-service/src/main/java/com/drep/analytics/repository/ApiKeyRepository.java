package com.drep.analytics.repository;

import com.drep.analytics.domain.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    Optional<ApiKeyEntity> findByKeyHashAndActiveTrue(String keyHash);

    List<ApiKeyEntity> findByTenantIdAndActiveTrue(String tenantId);
}
