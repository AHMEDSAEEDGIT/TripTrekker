package com.triptrekker.modules.flightsearch.mapper;

import com.triptrekker.modules.flightapi.model.FlightOffer;
import com.triptrekker.modules.flightapi.model.FlightSearchData;
import com.triptrekker.modules.flightapi.model.Segment;
import com.triptrekker.modules.flightapi.model.Slice;
import com.triptrekker.modules.flightsearch.dto.FlightOfferResponse;
import com.triptrekker.modules.flightsearch.dto.FlightSearchResponse;
import com.triptrekker.modules.flightsearch.dto.SegmentResponse;
import com.triptrekker.modules.flightsearch.dto.SliceResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class FlightSearchResponseMapper {

    public FlightSearchResponse toResponse(FlightSearchData data) {
        return new FlightSearchResponse(
                mapOffers(data.getOffers()),
                data.getTotalCount(),
                data.getOrigin(),
                data.getDestination(),
                data.getCurrency()
        );
    }

    private List<FlightOfferResponse> mapOffers(List<FlightOffer> offers) {
        if (offers == null) return Collections.emptyList();
        return offers.stream().map(this::toOfferResponse).toList();
    }

    private FlightOfferResponse toOfferResponse(FlightOffer offer) {
        return new FlightOfferResponse(
                offer.getOfferId(),
                offer.getProvider(),
                offer.getExpiresAt(),
                offer.getTotalAmount(),
                offer.getPricePerPerson(),
                offer.getBaseAmount(),
                offer.getTaxAmount(),
                offer.getCurrency(),
                offer.getAirlineName(),
                offer.getAirlineIataCode(),
                offer.getIsRefundable(),
                offer.getIsChangeable(),
                offer.getTotalEmissionsKg(),
                offer.getTags(),
                mapSlices(offer.getSlices())
        );
    }

    private List<SliceResponse> mapSlices(List<Slice> slices) {
        if (slices == null) return Collections.emptyList();
        return slices.stream().map(this::toSliceResponse).toList();
    }

    private SliceResponse toSliceResponse(Slice slice) {
        return new SliceResponse(
                slice.getOrigin(),
                slice.getOriginName(),
                slice.getDestination(),
                slice.getDestinationName(),
                slice.getDepartureTime(),
                slice.getArrivalTime(),
                slice.getDuration(),
                slice.getDurationFormatted(),
                slice.getStopsCount(),
                slice.getStopAirportCodes(),
                slice.getLayoverDurations(),
                slice.getFareBrandName(),
                mapSegments(slice.getSegments())
        );
    }

    private List<SegmentResponse> mapSegments(List<Segment> segments) {
        if (segments == null) return Collections.emptyList();
        return segments.stream().map(this::toSegmentResponse).toList();
    }

    private SegmentResponse toSegmentResponse(Segment seg) {
        return new SegmentResponse(
                seg.getOrigin(),
                seg.getOriginName(),
                seg.getOriginTerminal(),
                seg.getDestination(),
                seg.getDestinationName(),
                seg.getDestinationTerminal(),
                seg.getDepartingAt(),
                seg.getArrivingAt(),
                seg.getDuration(),
                seg.getDurationFormatted(),
                seg.getCarrierIataCode(),
                seg.getCarrierName(),
                seg.getFlightNumber(),
                seg.getOperatingCarrierIataCode(),
                seg.getOperatingCarrierName(),
                seg.getAircraftIataCode(),
                seg.getAircraftName(),
                seg.getCabinClass(),
                seg.getCabinClassMarketingName(),
                seg.getCheckedBagsCount(),
                seg.getTechnicalStopsCount()
        );
    }
}