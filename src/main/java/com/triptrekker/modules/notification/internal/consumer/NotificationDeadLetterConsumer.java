package com.triptrekker.modules.notification.internal.consumer;

import com.triptrekker.modules.notification.internal.config.NotificationRabbitMqConfig;
import com.triptrekker.modules.notification.model.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationDeadLetterConsumer {

    @RabbitListener(queues = {
            NotificationRabbitMqConfig.EMAIL_DLQ,
            NotificationRabbitMqConfig.SMS_DLQ,
            NotificationRabbitMqConfig.SSE_DLQ
    })
    public void handleDeadLetter(NotificationMessage message) {
        log.error("Notification delivery failed permanently. type={} template={} recipient={}",
                message.type(), message.templateName(), message.recipientReference());
    }
}
