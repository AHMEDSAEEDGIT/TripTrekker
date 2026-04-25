package com.triptrekker.modules.flightsearch.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FlightOfferResponse(
        String offerId,
        String provider,
        Instant expiresAt,
        BigDecimal totalAmount,
        BigDecimal pricePerPerson,
        BigDecimal baseAmount,
        BigDecimal taxAmount,
        String currency,
        String airlineName,
        String airlineIataCode,
        Boolean isRefundable,
        Boolean isChangeable,
        String totalEmissionsKg,
        List<String> tags,
        List<SliceResponse> slices
) {}