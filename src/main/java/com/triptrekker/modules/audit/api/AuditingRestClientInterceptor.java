package com.triptrekker.modules.audit.api;

import org.jspecify.annotations.NullMarked;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * RestClient interceptor that audits HTTP calls for a specific vendor.
 *
 * <p>Each vendor's RestClient bean registers its own instance of this interceptor,
 * constructed with that vendor's {@link IntegrationVendor} value. At runtime the
 * interceptor checks {@link IntegrationAuditContext#isActive(IntegrationVendor)} —
 * if the caller has not activated auditing for this vendor, the request is passed
 * through untouched. This makes auditing opt-in per call site, not per interceptor.
 *
 * <p>When auditing is active, the response body is buffered so it can be both
 * recorded and still read by the RestClient deserialization pipeline.
 */
public class AuditingRestClientInterceptor implements ClientHttpRequestInterceptor {

    private final IntegrationVendor vendor;
    private final IntegrationAuditPublisher auditPublisher;

    public AuditingRestClientInterceptor(IntegrationVendor vendor, IntegrationAuditPublisher auditPublisher) {
        this.vendor = vendor;
        this.auditPublisher = auditPublisher;
    }

    @Override
    @NullMarked
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (!IntegrationAuditContext.isActive(vendor)) {
            return execution.execute(request, body);
        }
        return executeWithAudit(request, body, execution);
    }

    private ClientHttpResponse executeWithAudit(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        Instant start = Instant.now();
        String requestPayload = new String(body, StandardCharsets.UTF_8);

        try {
            ClientHttpResponse response = execution.execute(request, body);
            byte[] responseBytes = response.getBody().readAllBytes();
            long durationMs = Instant.now().toEpochMilli() - start.toEpochMilli();

            auditPublisher.audit(buildEvent(
                    request, requestPayload,
                    response.getStatusCode().value(), new String(responseBytes, StandardCharsets.UTF_8),
                    start, durationMs, response.getStatusCode().is2xxSuccessful()
            ));

            return new BufferedClientHttpResponse(response, responseBytes);
        } catch (IOException e) {
            long durationMs = Instant.now().toEpochMilli() - start.toEpochMilli();
            auditPublisher.audit(buildEvent(request, requestPayload, null, null, start, durationMs, false));
            throw e;
        }
    }

    private IntegrationAuditEvent buildEvent(
            HttpRequest request,
            String requestPayload,
            Integer httpStatus,
            String responsePayload,
            Instant occurredAt,
            long durationMs,
            boolean success
    ) {
        String actorTypeStr = MDC.get("actorType");
        ActorType actorType = actorTypeStr != null ? ActorType.valueOf(actorTypeStr) : ActorType.GUEST;

        return new IntegrationAuditEvent(
                MDC.get("correlationId"),
                MDC.get("actorId"),
                actorType,
                vendor,
                request.getURI().getPath(),
                request.getMethod().name(),
                httpStatus,
                requestPayload,
                responsePayload,
                occurredAt,
                durationMs,
                success
        );
    }

    private record BufferedClientHttpResponse(ClientHttpResponse delegate, byte[] bufferedBody)
            implements ClientHttpResponse {

        @Override
        @NullMarked
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        @NullMarked
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        @NullMarked
        public InputStream getBody() {
            return new ByteArrayInputStream(bufferedBody);
        }

        @Override
        @NullMarked
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }
    }
}