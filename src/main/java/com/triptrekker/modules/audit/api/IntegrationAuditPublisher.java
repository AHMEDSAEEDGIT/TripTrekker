package com.triptrekker.modules.audit.api;

public interface IntegrationAuditPublisher {
    void audit(IntegrationAuditEvent event);
}