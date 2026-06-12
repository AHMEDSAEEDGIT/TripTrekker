package com.triptrekker.modules.audit.internal.entity;

import com.triptrekker.modules.audit.api.ActorType;
import com.triptrekker.modules.audit.api.IntegrationVendor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class IntegrationAuditLog {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", insertable = false)
    private UUID id;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "actor_id")
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 20)
    private ActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor", length = 50, nullable = false)
    private IntegrationVendor vendor;

    @Column(name = "api_endpoint", length = 500, nullable = false)
    private String apiEndpoint;

    @Column(name = "http_method", length = 10, nullable = false)
    private String httpMethod;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
