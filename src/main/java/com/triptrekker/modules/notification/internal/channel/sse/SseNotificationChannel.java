package com.triptrekker.modules.notification.internal.channel.sse;

import com.triptrekker.modules.notification.internal.channel.NotificationChannel;
import com.triptrekker.modules.notification.model.NotificationRequest;
import com.triptrekker.modules.notification.model.NotificationType;
import com.triptrekker.modules.notification.model.RenderedTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SseNotificationChannel implements NotificationChannel {

    private final SseConnectionRegistry registry;

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SSE;
    }

    @Override
    public boolean requiresHeader() {
        return false;
    }

    @Override
    public void deliver(String recipient, RenderedTemplate rendered, NotificationRequest request) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("template", request.getTemplateName());
        event.put("body", rendered.body());
        if (request.getTransactionData() != null && !request.getTransactionData().isEmpty()) {
            event.put("transactionData", request.getTransactionData());
        }
        registry.send(recipient, event);
    }
}
