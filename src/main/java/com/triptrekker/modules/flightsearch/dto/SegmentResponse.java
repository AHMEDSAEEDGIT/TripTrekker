package com.triptrekker.modules.flightsearch.dto;

public record SegmentResponse(
        String origin,
        String originName,
        String originTerminal,
        String destination,
        String destinationName,
        String destinationTerminal,
        String departingAt,
        String arrivingAt,
        String duration,
        String durationFormatted,
        String carrierIataCode,
        String carrierName,
        String flightNumber,
        String operatingCarrierIataCode,
        String operatingCarrierName,
        String aircraftIataCode,
        String aircraftName,
        String cabinClass,
        String cabinClassMarketingName,
        int checkedBagsCount,
        int technicalStopsCount
) {}