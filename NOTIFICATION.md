# Notification Module

The notification module lives at `com.triptrekker.modules.notification` and owns asynchronous delivery for user-facing notifications.

Other modules should only depend on the public API:

```java
com.triptrekker.modules.notification.api.NotificationPublisher
```

Do not import anything from `notification.internal`.

---

## What It Does

The module accepts a `NotificationMessage`, publishes it to RabbitMQ, consumes it by channel, renders a Mustache template, and sends it through the selected delivery channel.

Supported channels:

| Channel | Transport | Header required |
|---|---|---|
| `EMAIL` | SMTP through Spring `JavaMailSender` | Yes |
| `SMS` | Log stub by default, httpSMS when enabled | No |
| `SSE` | Spring `SseEmitter` stream | No |

The module does **not** persist notification delivery state. Delivery/outbox state is expected to be handled by the separate outbox implementation.

---

## Key Classes

| Class | Package | Role |
|---|---|---|
| `NotificationPublisher` | `api` | Public port used by other modules |
| `NotificationPublisherImpl` | `internal/publisher` | Publishes messages to RabbitMQ |
| `NotificationMessage` | `model` | RabbitMQ wire payload |
| `NotificationRequest` | `model` | Internal command object used after consumption |
| `NotificationType` | `model` | Channel enum: `EMAIL`, `SMS`, `SSE` |
| `TemplateName` | `model` | Logical template enum |
| `NotificationRabbitMqConfig` | `internal/config` | Exchanges, queues, bindings, DLQs, httpSMS `RestClient` |
| `NotificationService` | `internal/service` | Finds channel, renders template, delivers |
| `NotificationChannel` | `internal/channel` | Channel abstraction |
| `MustacheTemplateRenderer` | `internal/template` | Renders channel-scoped Mustache templates |
| `EmailNotificationConsumer` | `internal/consumer` | Consumes email queue |
| `SmsNotificationConsumer` | `internal/consumer` | Consumes SMS queue |
| `SseNotificationConsumer` | `internal/consumer` | Consumes SSE queue |
| `NotificationDeadLetterConsumer` | `internal/consumer` | Logs messages that reached notification DLQs |
| `RabbitListenerFailureConfig` | `common/config` | Logs final listener failure after retries, rejects without requeue |

---

## Full Flow

```
Booking / Payment / other module
        │
        ▼
NotificationPublisher.publish(NotificationMessage)
        │
        ▼
NotificationPublisherImpl
  - Picks routing key from NotificationMessage.type()
  - Sends JSON message to exchange: triptrekker.notifications
        │
        ▼
RabbitMQ topic exchange
  - notification.email → triptrekker.notifications.email.queue
  - notification.sms   → triptrekker.notifications.sms.queue
  - notification.sse   → triptrekker.notifications.sse.queue
        │
        ▼
Channel consumer
  - EmailNotificationConsumer / SmsNotificationConsumer / SseNotificationConsumer
  - Converts NotificationMessage to NotificationRequest
        │
        ▼
NotificationService.send(NotificationRequest)
  - Finds NotificationChannel by request.type
  - Asks channel whether header is required
  - Renders templates with MustacheTemplateRenderer
  - Passes transactionData to the channel without rendering it
        │
        ▼
NotificationChannel.deliver(...)
  - EMAIL: JavaMailSender
  - SMS: SmsSenderProvider
  - SSE: SseConnectionRegistry
```

---

## RabbitMQ Topology

Main exchange:

```
Exchange: triptrekker.notifications  (topic, durable)

Routing key          Queue
-----------          -----
notification.email   triptrekker.notifications.email.queue
notification.sms     triptrekker.notifications.sms.queue
notification.sse     triptrekker.notifications.sse.queue
```

Dead-letter exchange:

```
Exchange: triptrekker.notifications.dlx  (direct, durable)

DL routing key             DLQ
--------------             ---
notification.email.dead    triptrekker.notifications.email.dlq
notification.sms.dead      triptrekker.notifications.sms.dlq
notification.sse.dead      triptrekker.notifications.sse.dlq
```

Each main queue is configured with:

```text
x-dead-letter-exchange    = triptrekker.notifications.dlx
x-dead-letter-routing-key = channel-specific *.dead key
```

---

## Failure Behavior

Rabbit listener retry is configured in `application.yaml`:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        default-requeue-rejected: false
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000
          multiplier: 2
          max-interval: 10000
```

Behavior:

1. A consumer throws an exception.
2. Spring AMQP retries up to 3 attempts.
3. After retries are exhausted, `RabbitListenerFailureConfig` logs one final error with exchange, routing key, queue, message ID, and correlation ID.
4. The message is rejected with `requeue=false`.
5. RabbitMQ routes it to the configured DLQ.
6. `NotificationDeadLetterConsumer` logs that the message reached a notification DLQ.

This avoids infinite reprocessing loops. If DLQ routing is broken at the broker level, RabbitMQ may drop the message; for notifications this is acceptable as a final fallback, while the outbox remains the business source of truth.

---

## Template Rendering

Templates live under:

```text
src/main/resources/mustache/
```

Channel-specific directories:

```text
mustache/email/
mustache/sms/
mustache/sse/
```

`TemplateName` derives filenames from enum names:

```java
BOOKING_CONFIRMED
```

becomes:

```text
booking_confirmed_header.mustache
booking_confirmed_body.mustache
```

Email renders both header and body:

```text
mustache/email/booking_confirmed_header.mustache
mustache/email/booking_confirmed_body.mustache
```

SMS and SSE render only body:

```text
mustache/sms/booking_confirmed_body.mustache
mustache/sse/booking_confirmed_body.mustache
```

The `NotificationChannel.requiresHeader()` method decides whether the renderer loads the header template.

---

## How To Publish A Notification From Another Module

### Step 1 - Declare Module Dependency

In the sending module's `package-info.java`, allow dependency on `notification`:

```java
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "notification"}
)
package com.triptrekker.modules.booking;
```

### Step 2 - Inject The Public API

```java
import com.triptrekker.modules.notification.api.NotificationPublisher;
import com.triptrekker.modules.notification.model.NotificationMessage;
import com.triptrekker.modules.notification.model.NotificationType;
import com.triptrekker.modules.notification.model.TemplateName;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

class BookingNotificationSender {

    private final NotificationPublisher notificationPublisher;

    BookingNotificationSender(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    void bookingConfirmed(String email) {
        notificationPublisher.publish(new NotificationMessage(
                NotificationType.EMAIL,
                TemplateName.BOOKING_CONFIRMED,
                email,
                Map.of(
                        "passengerName", "Adele",
                        "flightNumber", "MS985",
                        "origin", "CAI",
                        "destination", "JFK",
                        "departureTime", "2026-06-15T10:30:00Z"
                ),
                Map.of(
                        "redirectType", "BOOKING",
                        "redirectId", "booking-123"
                ),
                UUID.randomUUID().toString(),
                Instant.now()
        ));
    }
}
```

Only import `notification.api` and `notification.model` from other modules.

`payload` is for Mustache template variables. `transactionData` is for channel-specific metadata that should not be rendered into templates, such as redirect identifiers for SSE or push clients.

---

## How To Add A New Notification Template

Example: add `CHECK_IN_OPENED`.

### Step 1 - Add Enum Value

Edit `TemplateName`:

```java
public enum TemplateName {
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    FLIGHT_REMINDER,
    CHECK_IN_OPENED;
}
```

### Step 2 - Add Email Templates

Create:

```text
src/main/resources/mustache/email/check_in_opened_header.mustache
src/main/resources/mustache/email/check_in_opened_body.mustache
```

Example header:

```text
Check-in is open - {{flightNumber}}
```

Example body:

```html
<h1>Hi {{passengerName}},</h1>
<p>Check-in is now open for flight <strong>{{flightNumber}}</strong>.</p>
```

### Step 3 - Add SMS Template If SMS Is Supported

Create:

```text
src/main/resources/mustache/sms/check_in_opened_body.mustache
```

Example:

```text
TripTrekker check-in is open for flight {{flightNumber}}.
```

### Step 4 - Add SSE Template If SSE Is Supported

Create:

```text
src/main/resources/mustache/sse/check_in_opened_body.mustache
```

Example:

```text
Check-in is open for flight {{flightNumber}}.
```

### Step 5 - Publish It

Use `TemplateName.CHECK_IN_OPENED` and choose the channel with `NotificationType`.

If you publish `EMAIL`, the email header and body files must exist.
If you publish `SMS` or `SSE`, only the body file must exist.

---

## How To Add A New Channel

Example: add `PUSH`.

### Step 1 - Add Notification Type

```java
public enum NotificationType {
    EMAIL,
    SMS,
    SSE,
    PUSH
}
```

### Step 2 - Add RabbitMQ Constants, Queue, DLQ, And Bindings

Edit `NotificationRabbitMqConfig`:

```java
public static final String PUSH_QUEUE = "triptrekker.notifications.push.queue";
public static final String PUSH_ROUTING_KEY = "notification.push";
public static final String PUSH_DLQ = "triptrekker.notifications.push.dlq";
public static final String PUSH_DL_ROUTING_KEY = "notification.push.dead";
```

Then add:

```java
@Bean Queue pushQueue()
@Bean Queue pushDeadLetterQueue()
@Bean Binding pushBinding()
@Bean Binding pushDeadLetterBinding()
```

Use the same pattern as email, SMS, and SSE.

### Step 3 - Route Publisher Messages

Edit `NotificationPublisherImpl`:

```java
case PUSH -> NotificationRabbitMqConfig.PUSH_ROUTING_KEY;
```

### Step 4 - Add A Consumer

Create:

```text
modules/notification/internal/consumer/PushNotificationConsumer.java
```

Pattern:

```java
@Component
@RequiredArgsConstructor
class PushNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = NotificationRabbitMqConfig.PUSH_QUEUE)
    void consume(NotificationMessage message) {
        notificationService.send(NotificationConsumerSupport.toRequest(message));
    }
}
```

### Step 5 - Implement NotificationChannel

Create:

```text
modules/notification/internal/channel/push/PushNotificationChannel.java
```

Pattern:

```java
@Component
@RequiredArgsConstructor
class PushNotificationChannel implements NotificationChannel {

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.PUSH;
    }

    @Override
    public boolean requiresHeader() {
        return true;
    }

    @Override
    public void deliver(String recipient, RenderedTemplate rendered, NotificationRequest request) {
        // Call push provider here.
    }
}
```

### Step 6 - Add Templates

Create:

```text
src/main/resources/mustache/push/{template}_header.mustache
src/main/resources/mustache/push/{template}_body.mustache
```

If the new channel does not need a header, return `false` from `requiresHeader()` and only create body templates.

---

## Local Configuration

Email:

```yaml
spring:
  mail:
    host: smtp-relay.brevo.com
    port: 587
    username: ${BREVO_SMTP_USERNAME}
    password: ${BREVO_SMTP_PASSWORD}

triptrekker:
  notification:
    email:
      from-email: ${NOTIFICATION_FROM_EMAIL:noreply@triptrekker.com}
      from-name: ${NOTIFICATION_FROM_NAME:TripTrekker}
```

SMS:

```yaml
triptrekker:
  notification:
    sms:
      provider: ${SMS_PROVIDER:log}
      httpsms:
        api-key: ${HTTPSMS_API_KEY:}
        from-number: ${HTTPSMS_FROM_NUMBER:}
        base-url: https://api.httpsms.com
```

By default, SMS uses `LogSmsSenderProvider`, so local development does not send real SMS messages.

To enable httpSMS:

```properties
SMS_PROVIDER=httpsms
HTTPSMS_API_KEY=...
HTTPSMS_FROM_NUMBER=...
```

---

## SSE Usage

Clients connect to:

```text
GET /api/v1/notifications/stream?recipientId={recipientId}
```

Server-sent events are emitted with event name:

```text
notification
```

The SSE channel sends:

```json
{
  "template": "BOOKING_CONFIRMED",
  "body": "Your booking is confirmed for flight MS985."
}
```

`recipientReference` for SSE messages must match the `recipientId` used by the client stream.

---

## Operational Notes

- RabbitMQ topology is declared by `NotificationRabbitMqConfig`.
- JSON conversion is shared through `common/config/RabbitMqConfig`.
- Listener retry and no-requeue behavior are configured in `application.yaml`.
- Final listener failure logging is handled by `RabbitListenerFailureConfig`.
- DLQ messages are logged by `NotificationDeadLetterConsumer`.
- Notification delivery state is intentionally not stored in this module; the outbox owns business-level delivery state.
