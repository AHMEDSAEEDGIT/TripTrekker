package com.triptrekker.modules.notification.internal.publisher;

import com.triptrekker.modules.notification.api.NotificationPublisher;
import com.triptrekker.modules.notification.internal.config.NotificationRabbitMqConfig;
import com.triptrekker.modules.notification.model.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPublisherImpl implements NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(NotificationMessage message) {
        String routingKey = switch (message.type()) {
            case EMAIL -> NotificationRabbitMqConfig.EMAIL_ROUTING_KEY;
            case SMS -> NotificationRabbitMqConfig.SMS_ROUTING_KEY;
            case SSE -> NotificationRabbitMqConfig.SSE_ROUTING_KEY;
        };
        rabbitTemplate.convertAndSend(NotificationRabbitMqConfig.NOTIFICATION_EXCHANGE, routingKey, message);
    }
}
