package com.triptrekker.modules.flightapi.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Slice {

    // ── Route ────────────────────────────────────────────────────────────────
    String origin;                      // IATA code (e.g. "CAI")
    String originName;                  // Airport name (e.g. "Cairo International")
    String destination;                 // IATA code (e.g. "LHR")
    String destinationName;             // Airport name (e.g. "Heathrow")

    // ── Times ────────────────────────────────────────────────────────────────
    String departureTime;               // ISO datetime from first segment
    String arrivalTime;                 // ISO datetime from last segment
    String duration;                    // ISO 8601 total duration
    String durationFormatted;           // Human-readable (e.g. "10h 15m")

    // ── Stops / connections ──────────────────────────────────────────────────
    int stopsCount;                     // 0 = Direct, 1+ = connections (segments - 1)
    List<String> stopAirportCodes;      // IATA codes of connection airports (empty if direct)
    List<String> layoverDurations;      // Formatted layover at each connection (e.g. ["2h 15m"])

    // ── Fare info ────────────────────────────────────────────────────────────
    String fareBrandName;               // Fare family name (e.g. "Light", "Flex"), null if not provided

    List<Segment> segments;
}
