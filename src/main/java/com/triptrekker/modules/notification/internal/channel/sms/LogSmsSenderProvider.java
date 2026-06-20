package com.triptrekker.modules.notification.internal.channel.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "triptrekker.notification.sms.provider", havingValue = "log", matchIfMissing = true)
public class LogSmsSenderProvider implements SmsSenderProvider {

    @Override
    public void send(String toPhoneNumber, String body) {
        log.info("[SMS-STUB] To: {} | Body: {}", toPhoneNumber, body);
    }
}
