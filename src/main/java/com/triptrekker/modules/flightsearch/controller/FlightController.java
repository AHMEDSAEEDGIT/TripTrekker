package com.triptrekker.modules.flightsearch.controller;

import com.triptrekker.modules.flightsearch.dto.FlightSearchResponse;
import com.triptrekker.modules.flightsearch.facade.FlightFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flight")
@RequiredArgsConstructor
public class FlightController {

    private final FlightFacade flightFacade;

    @GetMapping("/search")
    public FlightSearchResponse searchFlights(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String departureDate,
            @RequestParam(required = false) String returnDate,
            @RequestParam(defaultValue = "1") int adults,
            @RequestParam(defaultValue = "0") int children,
            @RequestParam(required = false) List<Integer> childAges,
            @RequestParam(defaultValue = "0") int infants,
            @RequestParam(defaultValue = "economy") String cabinClass,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestParam(required = false) String userId
    ) {
        return flightFacade.searchFlights(
                request, response,
                origin, destination, departureDate, returnDate,
                adults, children, childAges, infants,
                cabinClass, currency, userId);
    }
}
