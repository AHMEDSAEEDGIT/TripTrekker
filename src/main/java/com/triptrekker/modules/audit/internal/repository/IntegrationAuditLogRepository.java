package com.triptrekker.modules.audit.internal.repository;

import com.triptrekker.modules.audit.internal.entity.IntegrationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationAuditLogRepository extends JpaRepository<IntegrationAuditLog, Long> {
}