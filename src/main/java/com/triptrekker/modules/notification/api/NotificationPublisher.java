package com.triptrekker.modules.notification.api;

import com.triptrekker.modules.notification.model.NotificationMessage;

public interface NotificationPublisher {

    void publish(NotificationMessage message);
}
