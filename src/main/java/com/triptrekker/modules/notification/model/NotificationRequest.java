package com.triptrekker.modules.notification.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationRequest {

    TemplateName templateName;
    NotificationType type;

    /**
     * Channel-specific recipient reference.
     * EMAIL - email address. SMS - E.164 phone number. SSE - recipientId (userId or sessionId).
     */
    String recipientReference;

    Map<String, Object> payload;

    /**
     * Channel-specific transport metadata, such as push/SSE redirect identifiers.
     * These values are not used for template rendering.
     */
    Map<String, Object> transactionData;
}
