package com.triptrekker.modules.audit.internal.messaging;

import com.triptrekker.modules.audit.api.IntegrationAuditEvent;
import com.triptrekker.modules.audit.internal.config.AuditMessagingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class RabbitMqIntegrationAuditPublisher {

    private final RabbitTemplate rabbitTemplate;

    void publish(IntegrationAuditEvent event) {
        rabbitTemplate.convertAndSend(
                AuditMessagingConfig.EXCHANGE,
                AuditMessagingConfig.ROUTING_KEY,
                event
        );
        log.debug("Integration audit event published to RabbitMQ: vendor={} endpoint={}", event.vendor(), event.apiEndpoint());
    }
}