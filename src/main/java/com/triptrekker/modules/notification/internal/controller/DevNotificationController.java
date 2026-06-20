package com.triptrekker.modules.notification.internal.controller;

import com.triptrekker.modules.notification.api.NotificationPublisher;
import com.triptrekker.modules.notification.model.NotificationMessage;
import com.triptrekker.modules.notification.model.NotificationType;
import com.triptrekker.modules.notification.model.TemplateName;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * TEMPORARY verification-only endpoint for manually firing notifications of any
 * type/template through the real publish → RabbitMQ → consumer → channel pipeline.
 *
 * <p>Gated behind the {@code dev} profile. Remove once the notification module has
 * been verified and is driven by real producers (booking, payment, etc.).
 */
@RestController
@RequestMapping("/api/v1/dev/notifications")
@Profile("dev")
@RequiredArgsConstructor
public class DevNotificationController {

    private final NotificationPublisher notificationPublisher;

    public record TriggerRequest(
            NotificationType type,
            TemplateName templateName,
            String recipientReference,
            Map<String, Object> payload,
            Map<String, Object> transactionData
    ) {
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> trigger(@RequestBody TriggerRequest request) {
        NotificationMessage message = new NotificationMessage(
                request.type(),
                request.templateName(),
                request.recipientReference(),
                request.payload(),
                request.transactionData(),
                MDC.get("correlationId"),
                Instant.now()
        );
        notificationPublisher.publish(message);
        return ResponseEntity.accepted().body(Map.of(
                "published", true,
                "type", request.type(),
                "templateName", request.templateName(),
                "recipientReference", request.recipientReference()
        ));
    }
}