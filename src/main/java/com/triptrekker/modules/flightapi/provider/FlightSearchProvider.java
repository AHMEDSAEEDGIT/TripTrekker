package com.triptrekker.modules.flightapi.provider;


import com.triptrekker.modules.flightapi.model.FlightSearchCriteria;
import com.triptrekker.modules.flightapi.model.FlightSearchData;

public interface FlightSearchProvider {

    FlightSearchData searchFlights(FlightSearchCriteria criteria);
}
