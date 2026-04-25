package com.triptrekker.modules.flightsearch.dto;

import java.util.List;

public record FlightSearchResponse(
        List<FlightOfferResponse> offers,
        int totalCount,
        String origin,
        String destination,
        String currency
) {}