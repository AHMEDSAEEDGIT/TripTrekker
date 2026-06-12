package com.triptrekker.modules.audit.internal.aspect;

import com.triptrekker.modules.audit.api.AuditedIntegration;
import com.triptrekker.modules.audit.api.IntegrationAuditContext;
import com.triptrekker.modules.audit.api.IntegrationVendor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
class IntegrationAuditAspect {

    @Around("@annotation(auditedIntegration)")
    public Object around(ProceedingJoinPoint pjp, AuditedIntegration auditedIntegration) throws Throwable {
        for (IntegrationVendor vendor : auditedIntegration.vendors()) {
            IntegrationAuditContext.begin(vendor);
        }
        try {
            return pjp.proceed();
        } finally {
            for (IntegrationVendor vendor : auditedIntegration.vendors()) {
                IntegrationAuditContext.end(vendor);
            }
        }
    }
}