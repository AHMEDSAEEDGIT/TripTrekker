package com.triptrekker.modules.audit.internal.aspect;

import com.triptrekker.modules.audit.api.ActorType;
import com.triptrekker.modules.audit.api.AuditAction;
import com.triptrekker.modules.audit.api.Auditable;
import com.triptrekker.modules.audit.internal.AuditLog;
import com.triptrekker.modules.audit.internal.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditableAspect {

    private final AuditService auditService;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String oldValue = null;
        if (auditable.action() == AuditAction.UPDATE || auditable.action() == AuditAction.DELETE) {
            oldValue = auditService.toJsonMasked(pjp.getArgs()[0], auditable.sensitiveFields());
        }

        Object result = pjp.proceed();

        String newValue = null;
        if (auditable.action() == AuditAction.CREATE || auditable.action() == AuditAction.UPDATE) {
            newValue = auditService.toJsonMasked(result, auditable.sensitiveFields());
        }

        String module = auditable.module().isBlank()
                ? pjp.getSignature().getDeclaringType().getSimpleName()
                : auditable.module();

        auditService.log(AuditLog.builder()
                .entityType(auditable.entityType())
                .entityId(MDC.get("actorId"))
                .action(auditable.action())
                .actorId(MDC.get("actorId"))
                .actorType(ActorType.valueOf(MDC.get("actorType")))
                .oldValue(oldValue)
                .newValue(newValue)
                .module(module));

        return result;
    }
}