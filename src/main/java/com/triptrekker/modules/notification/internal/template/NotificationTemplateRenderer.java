package com.triptrekker.modules.notification.internal.template;

import com.triptrekker.modules.notification.model.NotificationType;
import com.triptrekker.modules.notification.model.RenderedTemplate;
import com.triptrekker.modules.notification.model.TemplateName;

import java.util.Map;

public interface NotificationTemplateRenderer {

    RenderedTemplate render(TemplateName templateName, NotificationType type, boolean includeHeader,
                            Map<String, Object> payload);
}
