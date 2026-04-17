package com.triptrekker.modules.flightapi.duffel.provider;

import com.triptrekker.modules.flightapi.duffel.client.DuffelFlightClient;
import com.triptrekker.modules.flightapi.duffel.mapper.DuffelFlightMapper;
import com.triptrekker.modules.flightapi.duffel.model.DuffelOfferRequestResponse;
import com.triptrekker.modules.flightapi.model.FlightSearchCriteria;
import com.triptrekker.modules.flightapi.model.FlightSearchData;
import com.triptrekker.modules.flightapi.provider.FlightSearchProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DuffelFlightSearchProvider implements FlightSearchProvider {

    private final DuffelFlightClient duffelFlightClient;
    private final DuffelFlightMapper duffelFlightMapper;

    @Override
    public FlightSearchData searchFlights(FlightSearchCriteria criteria) {
        log.info("Searching flights via Duffel: {} → {}, departure: {}", criteria.getOrigin(), criteria.getDestination(), criteria.getDepartureDate());
        DuffelOfferRequestResponse response = duffelFlightClient.createOfferRequest(criteria);
        log.info("Received {} offers from Duffel", response.getData().getOffers() == null ? 0 : response.getData().getOffers().size());
        return duffelFlightMapper.toFlightSearchData(response, criteria);
    }
}
