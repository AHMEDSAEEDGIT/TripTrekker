package com.triptrekker.modules.notification.internal.channel.sms;

import com.triptrekker.modules.notification.internal.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "triptrekker.notification.sms.provider", havingValue = "httpsms")
@RequiredArgsConstructor
public class HttpSmsSenderProvider implements SmsSenderProvider {

    private final NotificationProperties properties;
    private final RestClient httpSmsRestClient;

    @Override
    public void send(String toPhoneNumber, String body) {
        NotificationProperties.Sms.HttpSms config = properties.sms().httpsms();

        httpSmsRestClient.post()
                .uri(config.baseUrl() + "/v1/messages/send")
                .header("x-api-key", config.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "from", config.fromNumber(),
                        "to", toPhoneNumber,
                        "content", body
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
