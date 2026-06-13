package com.triptrekker.modules.notification.internal.service;

import com.triptrekker.modules.notification.internal.channel.NotificationChannel;
import com.triptrekker.modules.notification.internal.template.NotificationTemplateRenderer;
import com.triptrekker.modules.notification.model.NotificationRequest;
import com.triptrekker.modules.notification.model.RenderedTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final List<NotificationChannel> channels;
    private final NotificationTemplateRenderer templateRenderer;

    @Override
    public void send(NotificationRequest request) {
        NotificationChannel channel = channels.stream()
                .filter(c -> c.supports(request.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No notification channel registered for type: " + request.getType()
                ));

        RenderedTemplate rendered = templateRenderer.render(
                request.getTemplateName(),
                request.getType(),
                channel.requiresHeader(),
                request.getPayload()
        );

        channel.deliver(request.getRecipientReference(), rendered, request);
    }
}
