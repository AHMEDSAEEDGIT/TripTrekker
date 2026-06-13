package com.triptrekker.modules.notification.internal.channel.sms;

public interface SmsSenderProvider {

    void send(String toPhoneNumber, String body);
}
