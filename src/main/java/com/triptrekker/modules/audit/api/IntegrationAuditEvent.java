package com.triptrekker.modules.audit.api;

import java.time.Instant;

public record IntegrationAuditEvent(
        String correlationId,
        String actorId,
        ActorType actorType,
        IntegrationVendor vendor,
        String apiEndpoint,
        String httpMethod,
        Integer httpStatus,
        String requestPayload,
        String responsePayload,
        Instant occurredAt,
        long durationMs,
        boolean success
) {}