package com.triptrekker.common.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.UUID;

public class GuestUserIdGenerator {

    private static final String COOKIE_NAME = "guestUserId";
    private static final int ONE_YEAR_SECONDS = 365 * 24 * 60 * 60;

    private GuestUserIdGenerator() {
    }

    public static String generateGuestUserId(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> COOKIE_NAME.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElseGet(() -> createAndSetGuestCookie(response));
        }
        return createAndSetGuestCookie(response);
    }

    private static String createAndSetGuestCookie(HttpServletResponse response) {
        String guestUserId = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(COOKIE_NAME, guestUserId);
        cookie.setMaxAge(ONE_YEAR_SECONDS);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);
        return guestUserId;
    }
}
