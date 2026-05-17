package com.triptrekker.modules.audit.internal.messaging;

import com.triptrekker.modules.audit.api.IntegrationAuditEvent;
import com.triptrekker.modules.audit.api.IntegrationAuditPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ResilientIntegrationAuditPublisher implements IntegrationAuditPublisher {

    private final RabbitMqIntegrationAuditPublisher rabbitMqPublisher;
    private final SpringEventIntegrationAuditPublisher springEventPublisher;

    @Override
    public void audit(IntegrationAuditEvent event) {
        try {
            rabbitMqPublisher.publish(event);
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable, falling back to synchronous Spring Events for integration audit. Reason: {}", e.getMessage());
            springEventPublisher.publish(event);
        }
    }
}