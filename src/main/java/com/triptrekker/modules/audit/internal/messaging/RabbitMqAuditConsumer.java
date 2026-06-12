package com.triptrekker.modules.audit.internal.messaging;

import com.triptrekker.modules.audit.api.IntegrationAuditEvent;
import com.triptrekker.common.config.RabbitMqConfig;
import com.triptrekker.modules.audit.internal.service.IntegrationAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class RabbitMqAuditConsumer {

    private final IntegrationAuditLogService auditLogService;

    @RabbitListener(queues = RabbitMqConfig.INTEGRATION_AUDIT_QUEUE)
    void consume(IntegrationAuditEvent event) {
        log.debug("Integration audit event received from RabbitMQ: vendor={} endpoint={}", event.vendor(), event.apiEndpoint());
        auditLogService.save(event);
    }
}
