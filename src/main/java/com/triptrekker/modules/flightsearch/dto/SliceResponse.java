package com.triptrekker.modules.flightsearch.dto;

import java.util.List;

public record SliceResponse(
        String origin,
        String originName,
        String destination,
        String destinationName,
        String departureTime,
        String arrivalTime,
        String duration,
        String durationFormatted,
        int stopsCount,
        List<String> stopAirportCodes,
        List<String> layoverDurations,
        String fareBrandName,
        List<SegmentResponse> segments
) {}