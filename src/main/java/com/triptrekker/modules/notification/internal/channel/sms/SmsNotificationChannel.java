package com.triptrekker.modules.notification.internal.channel.sms;

import com.triptrekker.modules.notification.internal.channel.NotificationChannel;
import com.triptrekker.modules.notification.model.NotificationRequest;
import com.triptrekker.modules.notification.model.NotificationType;
import com.triptrekker.modules.notification.model.RenderedTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsNotificationChannel implements NotificationChannel {

    private final SmsSenderProvider smsSenderProvider;

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SMS;
    }

    @Override
    public boolean requiresHeader() {
        return false;
    }

    @Override
    public void deliver(String recipient, RenderedTemplate rendered, NotificationRequest request) {
        smsSenderProvider.send(recipient, rendered.body());
        log.info("SMS sent to: {}", recipient);
    }
}
