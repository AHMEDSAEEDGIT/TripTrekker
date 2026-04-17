package com.triptrekker.modules.flightapi.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FlightOffer {

    // ── Identity ─────────────────────────────────────────────────────────────
    String offerId;
    String provider;               // e.g. "DUFFEL" — distinguishes source when multiple providers are active
    Instant expiresAt;

    // ── Pricing ──────────────────────────────────────────────────────────────
    BigDecimal totalAmount;         // total for all passengers
    BigDecimal pricePerPerson;      // totalAmount / totalPassengers (shown on Skyscanner card)
    BigDecimal baseAmount;
    BigDecimal taxAmount;
    String currency;

    // ── Airline ──────────────────────────────────────────────────────────────
    String airlineName;
    String airlineIataCode;         // use to fetch airline logo on frontend

    // ── Conditions ───────────────────────────────────────────────────────────
    Boolean isRefundable;           // refund_before_departure.allowed
    Boolean isChangeable;           // change_before_departure.allowed

    // ── Sustainability ───────────────────────────────────────────────────────
    String totalEmissionsKg;        // CO₂ estimate, null if not provided by airline

    // ── Skyscanner tags ──────────────────────────────────────────────────────
    List<String> tags;              // ["CHEAPEST"], ["FASTEST"], ["BEST"] or combinations

    // ── Itinerary ────────────────────────────────────────────────────────────
    List<Slice> slices;             // 1 = one-way, 2 = round-trip
}
