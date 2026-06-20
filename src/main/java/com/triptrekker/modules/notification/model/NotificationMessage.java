package com.triptrekker.modules.notification.model;

import java.time.Instant;
import java.util.Map;

public record NotificationMessage(
        NotificationType type,
        TemplateName templateName,
        String recipientReference,
        Map<String, Object> payload,
        Map<String, Object> transactionData,
        String traceId,
        Instant publishedAt
) {
}
