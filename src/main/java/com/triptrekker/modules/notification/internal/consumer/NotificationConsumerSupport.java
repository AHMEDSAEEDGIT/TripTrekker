package com.triptrekker.modules.notification.internal.consumer;

import com.triptrekker.modules.notification.model.NotificationMessage;
import com.triptrekker.modules.notification.model.NotificationRequest;

final class NotificationConsumerSupport {

    private NotificationConsumerSupport() {
    }

    static NotificationRequest toRequest(NotificationMessage message) {
        return NotificationRequest.builder()
                .type(message.type())
                .templateName(message.templateName())
                .recipientReference(message.recipientReference())
                .payload(message.payload())
                .transactionData(message.transactionData())
                .build();
    }
}
