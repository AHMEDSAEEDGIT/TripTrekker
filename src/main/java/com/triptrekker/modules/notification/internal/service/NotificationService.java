package com.triptrekker.modules.notification.internal.service;

import com.triptrekker.modules.notification.model.NotificationRequest;

public interface NotificationService {

    void send(NotificationRequest request);
}
