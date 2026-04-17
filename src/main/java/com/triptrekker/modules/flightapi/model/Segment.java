package com.triptrekker.modules.flightapi.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Segment {

    // ── Airports ─────────────────────────────────────────────────────────────
    String origin;                      // IATA code (e.g. "CAI")
    String originName;                  // Full airport name
    String originTerminal;              // Terminal (e.g. "T1"), null if not provided
    String destination;                 // IATA code (e.g. "LHR")
    String destinationName;             // Full airport name
    String destinationTerminal;         // Terminal, null if not provided

    // ── Times ────────────────────────────────────────────────────────────────
    String departingAt;                 // ISO datetime (e.g. "2026-06-01T09:45:00")
    String arrivingAt;                  // ISO datetime
    String duration;                    // ISO 8601 (e.g. "PT7H30M")
    String durationFormatted;           // Human-readable (e.g. "7h 30m")

    // ── Marketing carrier (ticketing airline) ────────────────────────────────
    String carrierIataCode;             // e.g. "MS"
    String carrierName;                 // e.g. "EgyptAir"
    String flightNumber;                // e.g. "MS 777"

    // ── Operating carrier (actual aircraft operator, differs on codeshares) ──
    String operatingCarrierIataCode;    // null if same as marketing carrier
    String operatingCarrierName;

    // ── Aircraft ─────────────────────────────────────────────────────────────
    String aircraftIataCode;            // e.g. "738"
    String aircraftName;                // e.g. "Boeing 737-800"

    // ── Cabin ────────────────────────────────────────────────────────────────
    String cabinClass;                  // economy, business, first, premium_economy
    String cabinClassMarketingName;     // e.g. "Economy Light", "Business Flex"

    // ── Baggage ──────────────────────────────────────────────────────────────
    int checkedBagsCount;               // number of included checked bags (0 = none included)

    // ── Technical stops (within this single flight leg) ──────────────────────
    int technicalStopsCount;            // almost always 0 for commercial flights
}
