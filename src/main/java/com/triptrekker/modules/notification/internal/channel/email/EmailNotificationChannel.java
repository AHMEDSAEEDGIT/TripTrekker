package com.triptrekker.modules.notification.internal.channel.email;

import com.triptrekker.modules.notification.internal.channel.NotificationChannel;
import com.triptrekker.modules.notification.model.NotificationRequest;
import com.triptrekker.modules.notification.model.NotificationType;
import com.triptrekker.modules.notification.model.RenderedTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(EmailProperties.class)
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }

    @Override
    public boolean requiresHeader() {
        return true;
    }

    @Override
    public void deliver(String recipient, RenderedTemplate rendered, NotificationRequest request) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.fromEmail(), properties.fromName());
            helper.setTo(recipient);
            helper.setSubject(rendered.header());
            helper.setText(rendered.body(), true);
            mailSender.send(message);
            log.info("Email sent to: {}", recipient);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send email to: " + recipient, ex);
        }
    }
}
