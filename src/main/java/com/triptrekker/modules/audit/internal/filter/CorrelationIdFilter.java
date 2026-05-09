package com.triptrekker.modules.audit.internal.filter;

import com.triptrekker.common.utils.GuestUserIdGenerator;
import com.triptrekker.modules.audit.api.ActorType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_ID_HEADER = "X-User-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        MDC.put("correlationId", UUID.randomUUID().toString());
        response.setHeader(CORRELATION_ID_HEADER, MDC.get("correlationId"));

        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.isBlank()) {
            MDC.put("actorId", userId);
            MDC.put("actorType", ActorType.USER.name());
        } else {
            String guestId = GuestUserIdGenerator.generateGuestUserId(request, response);
            MDC.put("actorId", guestId);
            MDC.put("actorType", ActorType.GUEST.name());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}