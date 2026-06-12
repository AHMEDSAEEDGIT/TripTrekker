package com.triptrekker.modules.audit.internal;

import com.triptrekker.modules.audit.api.ActorType;
import org.hibernate.envers.RevisionListener;
import org.slf4j.MDC;

import java.util.UUID;

public class TripTrekkerRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        TripTrekkerRevision revision = (TripTrekkerRevision) revisionEntity;

        String actorTypeStr = MDC.get("actorType");
        String corrIdStr = MDC.get("correlationId");

        revision.setActorId(MDC.get("actorId"));
        revision.setActorType(actorTypeStr != null ? ActorType.valueOf(actorTypeStr) : ActorType.GUEST);
        revision.setCorrelationId(corrIdStr != null ? UUID.fromString(corrIdStr) : null);
    }
}