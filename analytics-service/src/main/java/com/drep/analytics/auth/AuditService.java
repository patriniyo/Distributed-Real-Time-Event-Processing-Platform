package com.drep.analytics.auth;

import com.drep.analytics.domain.AuditLogEntity;
import com.drep.analytics.repository.AuditLogRepository;
import com.drep.common.security.AuthenticatedPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public record AuditEntry(
            UUID id,
            UUID actorKeyId,
            String actorTenantId,
            String actorRole,
            String action,
            String resource,
            String details,
            Instant occurredAt
    ) {
    }

    public void log(AuthenticatedPrincipal actor, String action, String resource, String details) {
        AuditLogEntity entry = new AuditLogEntity();
        entry.setId(UUID.randomUUID());
        entry.setActorKeyId(actor.keyId());
        entry.setActorTenantId(actor.tenantId());
        entry.setActorRole(actor.role().name());
        entry.setAction(action);
        entry.setResource(resource);
        entry.setDetails(details);
        entry.setOccurredAt(Instant.now());
        auditLogRepository.save(entry);
    }

    public List<AuditEntry> recent(int limit) {
        return auditLogRepository.findByOrderByOccurredAtDesc(PageRequest.of(0, limit)).stream()
                .map(entry -> new AuditEntry(
                        entry.getId(),
                        entry.getActorKeyId(),
                        entry.getActorTenantId(),
                        entry.getActorRole(),
                        entry.getAction(),
                        entry.getResource(),
                        entry.getDetails(),
                        entry.getOccurredAt()))
                .toList();
    }
}
