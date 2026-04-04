package com.triptrekker.common.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private DateUtils() {
        // Private constructor to prevent instantiation
    }

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    public static boolean isDepartureBeforeReturn(LocalDate departure, LocalDate returnDate) {
        return departure.isBefore(returnDate);
    }
}
