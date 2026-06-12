package com.triptrekker.modules.audit.internal.messaging;

import com.triptrekker.modules.audit.api.IntegrationAuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class SpringEventIntegrationAuditPublisher {

    private final ApplicationEventPublisher eventPublisher;

    void publish(IntegrationAuditEvent event) {
        eventPublisher.publishEvent(event);
        log.debug("Integration audit event published via Spring Events (fallback): vendor={} endpoint={}", event.vendor(), event.apiEndpoint());
    }
}