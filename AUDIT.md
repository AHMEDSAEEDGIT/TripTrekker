# Audit Module

The audit module lives at `com.triptrekker.modules.audit` and provides two independent audit mechanisms that coexist without interfering with each other.

| Mechanism | What it tracks | Storage |
|---|---|---|
| **Hibernate Envers** | Entity field changes (who changed what in the DB) | `revinfo` table |
| **Integration Audit** | External API calls (request, response, duration, actor) | `integration_audit_log` table |

---

## Part 1 — Entity Change Audit (Hibernate Envers)

### What it does

Every time a JPA entity annotated with `@Audited` is created, updated, or deleted, Hibernate Envers records a revision entry that captures **who** triggered the change, **when**, and **which correlation ID** the request carried.

### How it works — full flow

```
Incoming HTTP request
        │
        ▼
CorrelationIdFilter                         (internal/filter/CorrelationIdFilter.java)
  - Reads X-Correlation-ID header (generates UUID if absent)
  - Reads X-User-ID header
    → if present:  MDC.put("actorId", userId),  MDC.put("actorType", "USER")
    → if absent:   MDC.put("actorId", guestId), MDC.put("actorType", "GUEST")
  - Sets X-Correlation-ID on the response
  - Clears MDC in finally block
        │
        ▼
Your service saves/updates/deletes a @Audited entity
        │
        ▼
Hibernate Envers intercepts the transaction
        │
        ▼
TripTrekkerRevisionListener.newRevision()   (internal/TripTrekkerRevisionListener.java)
  - Reads actorId, actorType, correlationId from MDC
  - Populates a TripTrekkerRevision instance
        │
        ▼
TripTrekkerRevision persisted to `revinfo`  (internal/TripTrekkerRevision.java)
  Columns: rev, rev_tstmp, actor_id, actor_type, correlation_id
```

### How to use it

Add `@Audited` to any JPA entity you want tracked. No other setup required.

```java
import org.hibernate.envers.Audited;

@Entity
@Audited
public class Booking {
    // Every save/update/delete on this entity is revision-tracked automatically
}
```

Envers creates a shadow table `booking_aud` and links each row change to a row in `revinfo` via the revision number.

### Actor identification

| Scenario | actorType | actorId |
|---|---|---|
| Request carries `X-User-ID` header | `USER` | The value of the header |
| No `X-User-ID` header | `GUEST` | UUID stored in a `guestUserId` cookie (created on first visit) |

---

## Part 2 — Integration Audit (External API Calls)

### What it does

Records every audited call to an external vendor API (Duffel, Stripe, etc.) including the full request payload, response payload, HTTP status, duration, and the actor who triggered it.

### Key classes at a glance

| Class | Package | Role |
|---|---|---|
| `AuditedIntegration` | `api` | Annotation — marks a method for auditing |
| `IntegrationVendor` | `api` | Enum — `DUFFEL`, `STRIPE`, `UNKNOWN` |
| `IntegrationAuditEvent` | `api` | Record — the audit data carrier |
| `IntegrationAuditPublisher` | `api` | Interface (port) — what callers depend on |
| `IntegrationAuditContext` | `api` | Thread-local — tracks which vendors are active |
| `AuditingRestClientInterceptor` | `api` | RestClient interceptor — captures HTTP request/response |
| `IntegrationAuditAspect` | `internal/aspect` | AOP advice — manages context lifecycle around annotated methods |
| `ResilientIntegrationAuditPublisher` | `internal/messaging` | `@Primary` publisher — tries RabbitMQ, falls back to Spring Events |
| `RabbitMqIntegrationAuditPublisher` | `internal/messaging` | Publishes event to RabbitMQ exchange |
| `SpringEventIntegrationAuditPublisher` | `internal/messaging` | Publishes event as Spring `ApplicationEvent` (fallback) |
| `RabbitMqAuditConsumer` | `internal/messaging` | `@RabbitListener` — receives from queue, saves to DB |
| `SpringEventAuditListener` | `internal/messaging` | `@EventListener` — receives fallback event, saves to DB |
| `IntegrationAuditLogService` | `internal/service` | Saves `IntegrationAuditLog` in a `REQUIRES_NEW` transaction |
| `IntegrationAuditLog` | `internal/entity` | JPA entity mapped to `integration_audit_log` |
| `RabbitMqConfig` | `common/config` | Declares RabbitMQ exchange, queue, DLQ, and message converter |

### How it works — full flow

#### Happy path (RabbitMQ is up)

```
Incoming HTTP request
        │
        ▼
CorrelationIdFilter                         (internal/filter/CorrelationIdFilter.java)
  - Populates MDC: correlationId, actorId, actorType
        │
        ▼
Method annotated with @AuditedIntegration is called
        │
        ▼
IntegrationAuditAspect.around()             (internal/aspect/IntegrationAuditAspect.java)
  - Iterates vendors[] from the annotation
  - Calls IntegrationAuditContext.begin(vendor) for each vendor
  - Proceeds with the method
  - Calls IntegrationAuditContext.end(vendor) in finally
        │
        ▼
RestClient executes the HTTP call
        │
        ▼
AuditingRestClientInterceptor.intercept()   (api/AuditingRestClientInterceptor.java)
  - Checks IntegrationAuditContext.isActive(this.vendor)
  - If NOT active → passthrough, nothing recorded
  - If active:
      · Captures request URI, method, body
      · Executes the HTTP call
      · Buffers response body (so RestClient can still read it)
      · Reads actorId, actorType, correlationId from MDC
      · Builds IntegrationAuditEvent record
      · Calls IntegrationAuditPublisher.audit(event)
        │
        ▼
ResilientIntegrationAuditPublisher.audit()  (internal/messaging/ResilientIntegrationAuditPublisher.java)
  - try:
        │
        ▼
    RabbitMqIntegrationAuditPublisher.publish()  (internal/messaging/RabbitMqIntegrationAuditPublisher.java)
      - Sends event as JSON to exchange: triptrekker.integration.audit
        │
        ▼
    RabbitMQ routes to queue: triptrekker.integration.audit.queue
        │
        ▼
    RabbitMqAuditConsumer.consume()         (internal/messaging/RabbitMqAuditConsumer.java)
      - Receives IntegrationAuditEvent from queue
        │
        ▼
    IntegrationAuditLogService.save()       (internal/service/IntegrationAuditLogService.java)
      - Opens a REQUIRES_NEW transaction (isolated from caller)
      - Maps event fields to IntegrationAuditLog entity
        │
        ▼
    integration_audit_log table (PostgreSQL)
```

#### Fallback path (RabbitMQ is down)

```
ResilientIntegrationAuditPublisher.audit()
  - try: RabbitMqIntegrationAuditPublisher.publish() → throws exception
  - catch: logs warning, switches to fallback
        │
        ▼
SpringEventIntegrationAuditPublisher.publish()  (internal/messaging/SpringEventIntegrationAuditPublisher.java)
  - Calls ApplicationEventPublisher.publishEvent(event)
  - Synchronous — runs in the same thread, same request
        │
        ▼
SpringEventAuditListener.onIntegrationAuditEvent()  (internal/messaging/SpringEventAuditListener.java)
  - @EventListener receives IntegrationAuditEvent
        │
        ▼
IntegrationAuditLogService.save()           (internal/service/IntegrationAuditLogService.java)
  - Same as the happy path — saves to integration_audit_log
```

### What gets recorded

| Column | Description |
|---|---|
| `correlation_id` | Request correlation ID from MDC |
| `actor_id` | User ID or guest UUID from MDC |
| `actor_type` | `USER` or `GUEST` |
| `vendor` | `DUFFEL`, `STRIPE`, or `UNKNOWN` |
| `api_endpoint` | URI path, e.g. `/air/orders` |
| `http_method` | `POST`, `GET`, etc. |
| `http_status` | HTTP response code — `null` if connection failed |
| `request_payload` | Raw JSON sent to the vendor |
| `response_payload` | Raw JSON received — `null` if IO failure |
| `duration_ms` | Total call duration in milliseconds |
| `success` | `true` if 2xx, `false` otherwise |
| `occurred_at` | Timestamp at call start (UTC) |

### RabbitMQ topology

```
Exchange : triptrekker.integration.audit        (direct, durable)
Queue    : triptrekker.integration.audit.queue  (durable)
Routing  : integration.audit

Dead Letter Exchange : triptrekker.integration.audit.dlx
Dead Letter Queue    : triptrekker.integration.audit.dlq
DL Routing           : integration.audit.dead
```

If a consumer fails to process a message, RabbitMQ routes it to the DLQ instead of losing it.

---

## How to add integration auditing to a new module

### Step 1 — Declare the module dependency

In your module's `package-info.java`, add `"audit"` to `allowedDependencies`:

```java
@ApplicationModule(allowedDependencies = {"common", "audit"})
package com.triptrekker.modules.yourmodule;
```

### Step 2 — Register the interceptor on your RestClient bean

Each vendor's RestClient gets its own `AuditingRestClientInterceptor` instance with the vendor identity baked in:

```java
@Bean
RestClient duffelRestClient(IntegrationAuditPublisher auditPublisher) {
    return RestClient.builder()
            .baseUrl(properties.baseUrl())
            .requestInterceptor(new AuditingRestClientInterceptor(IntegrationVendor.DUFFEL, auditPublisher))
            .build();
}
```

### Step 3 — Annotate the methods you want audited

Place `@AuditedIntegration` on any method — client class, service, or facade. Only the methods you annotate are audited. Unannotated methods using the same RestClient are passed through silently.

```java
// Single vendor
@AuditedIntegration(vendors = {IntegrationVendor.DUFFEL})
public OrderResponse createOrder(OrderRequest request) {
    return restClient.post().uri("/air/orders")...
}

// Multiple vendors in one method
@AuditedIntegration(vendors = {IntegrationVendor.DUFFEL, IntegrationVendor.STRIPE})
public BookingResult bookAndPay(BookingRequest request) {
    OrderResponse order = duffelClient.createOrder(request);
    ChargeResponse charge = stripeClient.capture(payment);
    return combine(order, charge);
}

// Not annotated — never audited, no performance overhead
public SearchResponse searchOffers(SearchRequest request) {
    return restClient.post().uri("/air/offer_requests") ...
}
```

### Adding a new vendor

Add an entry to `IntegrationVendor` (`api/IntegrationVendor.java`) and register a new `AuditingRestClientInterceptor` on that vendor's RestClient bean. No other changes needed.

---

## Database tables

### `revinfo` — Hibernate Envers revisions

```sql
rev            BIGINT    PRIMARY KEY
rev_tstmp      TIMESTAMP NOT NULL
actor_id       VARCHAR(255)
actor_type     VARCHAR(20)   -- USER | GUEST
correlation_id UUID
```

### `integration_audit_log` — Integration API calls

```sql
id               BIGINT    PRIMARY KEY
correlation_id   VARCHAR(36)
actor_id         VARCHAR(255)
actor_type       VARCHAR(20)
vendor           VARCHAR(50)   NOT NULL
api_endpoint     VARCHAR(500)  NOT NULL
http_method      VARCHAR(10)   NOT NULL
http_status      INTEGER
request_payload  TEXT
response_payload TEXT
duration_ms      BIGINT
success          BOOLEAN       NOT NULL
occurred_at      TIMESTAMPTZ   NOT NULL
```
