package com.triptrekker.modules.audit.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(AuditLog.AuditLogBuilder builder) {
        String corrIdStr = MDC.get("correlationId");
        auditLogRepository.save(builder
                .correlationId(corrIdStr != null ? UUID.fromString(corrIdStr) : null)
                .build());
    }

    public String toJsonMasked(Object obj, String... sensitiveFields) {
        if (obj == null) return null;
        try {
            var node = objectMapper.valueToTree(obj);
            if (node instanceof ObjectNode objectNode) {
                for (String field : sensitiveFields) {
                    if (objectNode.has(field)) {
                        objectNode.put(field, "***");
                    }
                }
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }
}