package com.triptrekker.modules.flightapi.duffel.client;

import com.triptrekker.modules.flightapi.duffel.config.DuffelProperties;
import com.triptrekker.modules.flightapi.duffel.exception.DuffelApiException;
import com.triptrekker.modules.flightapi.duffel.model.DuffelOfferRequestResponse;
import com.triptrekker.modules.flightapi.model.FlightSearchCriteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DuffelFlightClient {

    private static final String OFFER_REQUESTS_PATH = "/air/offer_requests";
    private static final String DUFFEL_VERSION = "v2";

    private final RestClient restClient;
    private final DuffelProperties duffelProperties;

    public DuffelFlightClient(
            @Qualifier("duffelRestClient") RestClient restClient,
            DuffelProperties duffelProperties) {
        this.restClient = restClient;
        this.duffelProperties = duffelProperties;
    }

    public DuffelOfferRequestResponse createOfferRequest(FlightSearchCriteria criteria) {
        Map<String, Object> requestBody = buildRequestBody(criteria);

        log.info("Creating Duffel offer request: {} → {} on {}, passengers: {} adults / {} children / {} infants",
                criteria.getOrigin(), criteria.getDestination(), criteria.getDepartureDate(),
                criteria.getAdults(), criteria.getChildren(), criteria.getInfants());

        DuffelOfferRequestResponse response = restClient.post()
                .uri(OFFER_REQUESTS_PATH)
                .header("Authorization", "Bearer " + duffelProperties.getApiToken())
                .header("Duffel-Version", DUFFEL_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (_, res) -> {
                    String body = StreamUtils.copyToString(res.getBody(), StandardCharsets.UTF_8);
                    log.error("Duffel offer request error [{}]: {}", res.getStatusCode(), body);
                    throw new DuffelApiException(res.getStatusCode().value(), body);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (_, res) -> {
                    log.error("Duffel upstream error [{}] while creating offer request", res.getStatusCode());
                    throw new DuffelApiException(res.getStatusCode().value(),
                            "Duffel upstream error " + res.getStatusCode());
                })
                .body(DuffelOfferRequestResponse.class);

        if (response == null || response.getData() == null) {
            throw new DuffelApiException(200, "Duffel returned an empty response for offer request");
        }
        return response;
    }

    private Map<String, Object> buildRequestBody(FlightSearchCriteria criteria) {
        List<Map<String, String>> slices = new ArrayList<>();

        Map<String, String> outboundSlice = new HashMap<>();
        outboundSlice.put("origin", criteria.getOrigin());
        outboundSlice.put("destination", criteria.getDestination());
        outboundSlice.put("departure_date", criteria.getDepartureDate().toString());
        slices.add(outboundSlice);

        if (criteria.isRoundTrip()) {
            Map<String, String> returnSlice = new HashMap<>();
            returnSlice.put("origin", criteria.getDestination());
            returnSlice.put("destination", criteria.getOrigin());
            returnSlice.put("departure_date", criteria.getReturnDate().toString());
            slices.add(returnSlice);
        }

        List<Map<String, Object>> passengers = buildPassengers(criteria);

        Map<String, Object> data = new HashMap<>();
        data.put("slices", slices);
        data.put("passengers", passengers);
        data.put("cabin_class", criteria.getCabinClass());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", data);
        return requestBody;
    }

    private List<Map<String, Object>> buildPassengers(FlightSearchCriteria criteria) {
        List<Map<String, Object>> passengers = new ArrayList<>();

        for (int i = 0; i < criteria.getAdults(); i++) {
            passengers.add(Map.of("type", "adult"));
        }

        List<Integer> childAges = criteria.getChildAges();
        for (int i = 0; i < criteria.getChildren(); i++) {
            int age = i < childAges.size() ? childAges.get(i) : 10;
            if (i >= childAges.size()) {
                log.warn("Age not provided for child passenger index {}; defaulting to 10", i);
            }
            passengers.add(Map.of("age", age));
        }

        for (int i = 0; i < criteria.getInfants(); i++) {
            passengers.add(Map.of("type", "infant_without_seat"));
        }

        return passengers;
    }
}
