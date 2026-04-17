package com.triptrekker.modules.flightapi.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FlightSearchCriteria {

    String origin;              // IATA airport code (e.g. "CAI", "LHR")
    String destination;         // IATA airport code
    LocalDate departureDate;
    LocalDate returnDate;       // null for one-way trips
    int adults;
    int children;               // number of children (ages required in childAges)
    List<Integer> childAges;    // age of each child (must match children count)
    int infants;                // lap infants (infant_without_seat)
    String cabinClass;          // economy, premium_economy, business, first
    String currency;

    public static FlightSearchCriteria create(
            String origin, String destination,
            String departureDate, String returnDate,
            int adults, int children, List<Integer> childAges, int infants,
            String cabinClass, String currency) {

        return FlightSearchCriteria.builder()
                .origin(origin.toUpperCase())
                .destination(destination.toUpperCase())
                .departureDate(LocalDate.parse(departureDate))
                .returnDate(returnDate != null && !returnDate.isBlank() ? LocalDate.parse(returnDate) : null)
                .adults(adults)
                .children(children)
                .childAges(childAges != null ? childAges : List.of())
                .infants(infants)
                .cabinClass(cabinClass.toLowerCase())
                .currency(currency.toUpperCase())
                .build();
    }

    public boolean isRoundTrip() {
        return returnDate != null;
    }

    public int getTotalPassengers() {
        return adults + children + infants;
    }

    public String toCacheKey() {
        String raw = origin + "|" + destination + "|" + departureDate + "|" + returnDate
                + "|" + adults + "|" + children + "|" + childAges + "|" + infants
                + "|" + cabinClass + "|" + currency;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
