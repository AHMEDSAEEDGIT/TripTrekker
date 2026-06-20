package com.triptrekker.modules.notification.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "triptrekker.notification")
public record NotificationProperties(Sms sms) {

    public record Sms(String provider, HttpSms httpsms) {

        public record HttpSms(String apiKey, String fromNumber, String baseUrl) {
        }
    }
}
