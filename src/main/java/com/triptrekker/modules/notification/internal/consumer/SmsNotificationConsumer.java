package com.triptrekker.modules.notification.internal.consumer;

import com.triptrekker.modules.notification.internal.config.NotificationRabbitMqConfig;
import com.triptrekker.modules.notification.internal.service.NotificationService;
import com.triptrekker.modules.notification.model.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = NotificationRabbitMqConfig.SMS_QUEUE)
    public void consume(NotificationMessage message) {
        notificationService.send(NotificationConsumerSupport.toRequest(message));
    }
}
