package com.triptrekker.modules.audit.internal;

import com.triptrekker.modules.audit.api.ActorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "revinfo")
@RevisionEntity(TripTrekkerRevisionListener.class)
@Getter
@Setter
public class TripTrekkerRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rev_seq")
    @SequenceGenerator(name = "rev_seq", sequenceName = "revinfo_rev_seq", allocationSize = 1)
    @RevisionNumber
    private long rev;

    @RevisionTimestamp
    @Column(name = "rev_tstmp", nullable = false)
    private Instant revtstmp;

    @Column(name = "actor_id", length = 255)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 20)
    private ActorType actorType;

    @Column(name = "correlation_id")
    private UUID correlationId;
}