package com.triptrekker.modules.notification.model;

import java.util.Locale;

public enum TemplateName {
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    FLIGHT_REMINDER;

    public String notificationName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String headerTemplateFileName() {
        return notificationName() + "_header.mustache";
    }

    public String bodyTemplateFileName() {
        return notificationName() + "_body.mustache";
    }
}
