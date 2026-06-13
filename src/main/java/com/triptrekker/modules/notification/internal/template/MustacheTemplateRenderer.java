package com.triptrekker.modules.notification.internal.template;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.triptrekker.modules.notification.model.NotificationType;
import com.triptrekker.modules.notification.model.RenderedTemplate;
import com.triptrekker.modules.notification.model.TemplateName;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.Map;

@Component
public class MustacheTemplateRenderer implements NotificationTemplateRenderer {

    private static final String ROOT = "mustache/";

    private final MustacheFactory factory;

    public MustacheTemplateRenderer() {
        this.factory = new DefaultMustacheFactory(ROOT);
    }

    @Override
    public RenderedTemplate render(TemplateName templateName, NotificationType type, boolean includeHeader,
                                   Map<String, Object> payload) {
        String channelDir = type.name().toLowerCase() + "/";
        String header = includeHeader
                ? execute(channelDir + templateName.headerTemplateFileName(), payload)
                : null;
        String body = execute(channelDir + templateName.bodyTemplateFileName(), payload);
        return new RenderedTemplate(header, body);
    }

    private String execute(String templatePath, Map<String, Object> payload) {
        try {
            var mustache = factory.compile(templatePath);
            var writer = new StringWriter();
            mustache.execute(writer, payload).flush();
            return writer.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to render notification template: " + templatePath, ex);
        }
    }
}
