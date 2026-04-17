package com.triptrekker.modules.flightsearch.service;

import com.triptrekker.modules.flightapi.model.FlightSearchCriteria;
import com.triptrekker.modules.flightapi.model.FlightSearchData;
import com.triptrekker.modules.flightapi.provider.FlightSearchProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightSearchProvider flightSearchProvider;

    @Cacheable(value = "flights", key = "'user:' + #userId + ':' + #criteria.toCacheKey()")
    public FlightSearchData searchFlightsForLoggedInUser(String userId, FlightSearchCriteria criteria) {
        return flightSearchProvider.searchFlights(criteria);
    }

    @Cacheable(value = "flights", key = "'guest:' + #guestId + ':' + #criteria.toCacheKey()")
    public FlightSearchData searchFlightsForGuestUser(String guestId, FlightSearchCriteria criteria) {
        return flightSearchProvider.searchFlights(criteria);
    }
}
