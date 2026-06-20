package com.triptrekker.modules.notification.internal.channel.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("triptrekker.notification.email")
public record EmailProperties(
        String fromEmail,
        @DefaultValue("TripTrekker") String fromName
) {
}
