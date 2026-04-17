package com.triptrekker.modules.flightapi.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FlightSearchData {

    List<FlightOffer> offers;
    int totalCount;
    String origin;
    String destination;
    String currency;
}
