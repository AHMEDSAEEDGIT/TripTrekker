package com.triptrekker.modules.notification.internal.channel;

import com.triptrekker.modules.notification.model.NotificationRequest;
import com.triptrekker.modules.notification.model.NotificationType;
import com.triptrekker.modules.notification.model.RenderedTemplate;

public interface NotificationChannel {

    boolean supports(NotificationType type);

    boolean requiresHeader();

    void deliver(String recipient, RenderedTemplate rendered, NotificationRequest request);
}
