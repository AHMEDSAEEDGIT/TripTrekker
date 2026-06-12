package com.triptrekker.modules.audit.internal.messaging;

import com.triptrekker.modules.audit.api.IntegrationAuditEvent;
import com.triptrekker.modules.audit.internal.service.IntegrationAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class SpringEventAuditListener {

    private final IntegrationAuditLogService auditLogService;

    @EventListener
    void onIntegrationAuditEvent(IntegrationAuditEvent event) {
        log.debug("Integration audit event received via Spring Events (fallback): vendor={} endpoint={}", event.vendor(), event.apiEndpoint());
        auditLogService.save(event);
    }
}