package com.triptrekker.modules.flightsearch.facade;

import com.triptrekker.common.utils.GuestUserIdGenerator;
import com.triptrekker.modules.flightapi.model.FlightSearchCriteria;
import com.triptrekker.modules.flightsearch.dto.FlightSearchResponse;
import com.triptrekker.modules.flightsearch.mapper.FlightSearchResponseMapper;
import com.triptrekker.modules.flightsearch.service.FlightService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlightFacade {

    private final FlightService flightService;
    private final FlightSearchResponseMapper mapper;

    public FlightSearchResponse searchFlights(
            HttpServletRequest request,
            HttpServletResponse response,
            String origin,
            String destination,
            String departureDate,
            String returnDate,
            int adults,
            int children,
            List<Integer> childAges,
            int infants,
            String cabinClass,
            String currency,
            String userId
    ) {
        FlightSearchCriteria criteria = FlightSearchCriteria.create(
                origin, destination, departureDate, returnDate,
                adults, children, childAges, infants,
                cabinClass, currency);

        if (userId != null && !userId.isEmpty()) {
            return mapper.toResponse(flightService.searchFlightsForLoggedInUser(userId, criteria));
        }

        String guestUserId = GuestUserIdGenerator.generateGuestUserId(request, response);
        log.info("guestUserId : {}", guestUserId);
        return mapper.toResponse(flightService.searchFlightsForGuestUser(guestUserId, criteria));
    }
}
