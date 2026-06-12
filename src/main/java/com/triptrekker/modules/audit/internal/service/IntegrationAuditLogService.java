package com.triptrekker.modules.audit.internal.service;

import com.triptrekker.modules.audit.api.IntegrationAuditEvent;
import com.triptrekker.modules.audit.internal.entity.IntegrationAuditLog;
import com.triptrekker.modules.audit.internal.repository.IntegrationAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationAuditLogService {

    private final IntegrationAuditLogRepository repository;

    /**
     * Persists the audit record in its own transaction so a caller rollback
     * never suppresses the audit trail.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(IntegrationAuditEvent event) {
        try {
            IntegrationAuditLog record = new IntegrationAuditLog();
            record.setCorrelationId(parseCorrelationId(event.correlationId()));
            record.setActorId(event.actorId());
            record.setActorType(event.actorType());
            record.setVendor(event.vendor());
            record.setApiEndpoint(event.apiEndpoint());
            record.setHttpMethod(event.httpMethod());
            record.setHttpStatus(event.httpStatus());
            record.setRequestPayload(event.requestPayload());
            record.setResponsePayload(event.responsePayload());
            record.setDurationMs(event.durationMs());
            record.setSuccess(event.success());
            record.setOccurredAt(event.occurredAt());
            repository.save(record);
        } catch (Exception e) {
            log.error("Failed to persist integration audit log: vendor={} endpoint={}", event.vendor(), event.apiEndpoint(), e);
        }
    }

    private UUID parseCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(correlationId);
        } catch (IllegalArgumentException e) {
            log.warn("Skipping invalid integration audit correlation id: {}", correlationId);
            return null;
        }
    }
}
