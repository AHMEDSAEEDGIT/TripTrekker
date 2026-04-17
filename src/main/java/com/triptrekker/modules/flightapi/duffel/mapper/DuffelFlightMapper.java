package com.triptrekker.modules.flightapi.duffel.mapper;

import com.triptrekker.modules.flightapi.duffel.model.DuffelOfferRequestResponse;
import com.triptrekker.modules.flightapi.duffel.model.DuffelOfferRequestResponse.*;
import com.triptrekker.modules.flightapi.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DuffelFlightMapper {

    private static final String PROVIDER = "DUFFEL";

    public FlightSearchData toFlightSearchData(DuffelOfferRequestResponse response, FlightSearchCriteria criteria) {
        List<Offer> rawOffers = response.getData().getOffers();
        if (rawOffers == null || rawOffers.isEmpty()) {
            return FlightSearchData.builder()
                    .offers(Collections.emptyList())
                    .totalCount(0)
                    .origin(criteria.getOrigin())
                    .destination(criteria.getDestination())
                    .currency(criteria.getCurrency())
                    .build();
        }

        int totalPassengers = Math.max(criteria.getTotalPassengers(), 1);

        // Pre-compute metrics for tag computation
        Map<String, BigDecimal> priceById = rawOffers.stream()
                .collect(Collectors.toMap(Offer::getId, o -> parseBigDecimal(o.getTotalAmount())));
        Map<String, Long> durationById = rawOffers.stream()
                .collect(Collectors.toMap(Offer::getId, o -> totalDurationMinutes(o.getSlices())));

        Set<String> cheapestIds = findMinValueIds(priceById);
        Set<String> fastestIds  = findMinValueIds(durationById);
        Set<String> bestIds     = computeBestIds(priceById, durationById);

        List<FlightOffer> offers = rawOffers.stream()
                .map(o -> toFlightOffer(o, totalPassengers, computeTags(o.getId(), cheapestIds, fastestIds, bestIds)))
                .toList();

        return FlightSearchData.builder()
                .offers(offers)
                .totalCount(offers.size())
                .origin(criteria.getOrigin())
                .destination(criteria.getDestination())
                .currency(criteria.getCurrency())
                .build();
    }

    // ── Offer ────────────────────────────────────────────────────────────────

    private FlightOffer toFlightOffer(Offer offer, int totalPassengers, List<String> tags) {
        BigDecimal totalAmount = parseBigDecimal(offer.getTotalAmount());
        BigDecimal pricePerPerson = totalAmount != null && totalPassengers > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalPassengers), 2, RoundingMode.HALF_UP)
                : totalAmount;

        OfferConditions conditions = offer.getConditions();
        Boolean isRefundable = conditions != null && conditions.getRefundBeforeDeparture() != null
                ? conditions.getRefundBeforeDeparture().getAllowed()
                : null;
        Boolean isChangeable = conditions != null && conditions.getChangeBeforeDeparture() != null
                ? conditions.getChangeBeforeDeparture().getAllowed()
                : null;

        return FlightOffer.builder()
                .offerId(offer.getId())
                .provider(PROVIDER)
                .expiresAt(parseInstant(offer.getExpiresAt()))
                .totalAmount(totalAmount)
                .pricePerPerson(pricePerPerson)
                .baseAmount(parseBigDecimal(offer.getBaseAmount()))
                .taxAmount(parseBigDecimal(offer.getTaxAmount()))
                .currency(offer.getTotalCurrency())
                .airlineName(offer.getOwner() != null ? offer.getOwner().getName() : null)
                .airlineIataCode(offer.getOwner() != null ? offer.getOwner().getIataCode() : null)
                .isRefundable(isRefundable)
                .isChangeable(isChangeable)
                .totalEmissionsKg(offer.getTotalEmissionsKg())
                .tags(tags)
                .slices(mapSlices(offer.getSlices()))
                .build();
    }

    // ── Slice ────────────────────────────────────────────────────────────────

    private List<Slice> mapSlices(List<OfferSlice> offerSlices) {
        if (offerSlices == null) return Collections.emptyList();
        return offerSlices.stream().map(this::toSlice).toList();
    }

    private Slice toSlice(OfferSlice offerSlice) {
        List<OfferSegment> segments = offerSlice.getSegments() != null ? offerSlice.getSegments() : List.of();

        String departureTime = segments.isEmpty() ? null : segments.getFirst().getDepartingAt();
        String arrivalTime   = segments.isEmpty() ? null : segments.getLast().getArrivingAt();
        int stopsCount       = Math.max(0, segments.size() - 1);

        // Connection airports: destination of every segment except the last
        List<String> stopAirportCodes = segments.stream()
                .limit(Math.max(0L, segments.size() - 1))
                .map(seg -> seg.getDestination() != null ? seg.getDestination().getIataCode() : null)
                .filter(Objects::nonNull)
                .toList();

        // Layover between consecutive segments
        List<String> layoverDurations = new ArrayList<>();
        for (int i = 0; i < segments.size() - 1; i++) {
            String layover = computeLayoverDuration(
                    segments.get(i).getArrivingAt(),
                    segments.get(i + 1).getDepartingAt());
            layoverDurations.add(layover);
        }

        String duration = offerSlice.getDuration();
        return Slice.builder()
                .origin(offerSlice.getOrigin() != null ? offerSlice.getOrigin().getIataCode() : null)
                .originName(offerSlice.getOrigin() != null ? offerSlice.getOrigin().getName() : null)
                .destination(offerSlice.getDestination() != null ? offerSlice.getDestination().getIataCode() : null)
                .destinationName(offerSlice.getDestination() != null ? offerSlice.getDestination().getName() : null)
                .departureTime(departureTime)
                .arrivalTime(arrivalTime)
                .duration(duration)
                .durationFormatted(formatDuration(duration))
                .stopsCount(stopsCount)
                .stopAirportCodes(stopAirportCodes)
                .layoverDurations(layoverDurations)
                .fareBrandName(offerSlice.getFareBrandName())
                .segments(mapSegments(segments))
                .build();
    }

    // ── Segment ──────────────────────────────────────────────────────────────

    private List<Segment> mapSegments(List<OfferSegment> offerSegments) {
        if (offerSegments == null) return Collections.emptyList();
        return offerSegments.stream().map(this::toSegment).toList();
    }

    private Segment toSegment(OfferSegment seg) {
        SegmentPassenger firstPassenger = (seg.getPassengers() != null && !seg.getPassengers().isEmpty())
                ? seg.getPassengers().getFirst() : null;

        String cabinClass             = firstPassenger != null ? firstPassenger.getCabinClass() : null;
        String cabinClassMarketingName = firstPassenger != null ? firstPassenger.getCabinClassMarketingName() : null;
        int checkedBagsCount          = resolveCheckedBags(firstPassenger);

        String marketingCode = seg.getMarketingCarrier() != null ? seg.getMarketingCarrier().getIataCode() : null;
        String operatingCode = seg.getOperatingCarrier() != null ? seg.getOperatingCarrier().getIataCode() : null;

        // Only expose operating carrier when it differs from marketing carrier (codeshare)
        boolean isCodeshare = operatingCode != null && !operatingCode.equals(marketingCode);

        String flightNumber = marketingCode != null && seg.getMarketingCarrierFlightNumber() != null
                ? marketingCode + " " + seg.getMarketingCarrierFlightNumber()
                : seg.getMarketingCarrierFlightNumber();

        String duration = seg.getDuration();
        int technicalStops = seg.getStops() != null ? seg.getStops().size() : 0;

        return Segment.builder()
                .origin(seg.getOrigin() != null ? seg.getOrigin().getIataCode() : null)
                .originName(seg.getOrigin() != null ? seg.getOrigin().getName() : null)
                .originTerminal(seg.getOriginTerminal())
                .destination(seg.getDestination() != null ? seg.getDestination().getIataCode() : null)
                .destinationName(seg.getDestination() != null ? seg.getDestination().getName() : null)
                .destinationTerminal(seg.getDestinationTerminal())
                .departingAt(seg.getDepartingAt())
                .arrivingAt(seg.getArrivingAt())
                .duration(duration)
                .durationFormatted(formatDuration(duration))
                .carrierIataCode(marketingCode)
                .carrierName(seg.getMarketingCarrier() != null ? seg.getMarketingCarrier().getName() : null)
                .flightNumber(flightNumber)
                .operatingCarrierIataCode(isCodeshare ? operatingCode : null)
                .operatingCarrierName(isCodeshare && seg.getOperatingCarrier() != null ? seg.getOperatingCarrier().getName() : null)
                .aircraftIataCode(seg.getAircraft() != null ? seg.getAircraft().getIataCode() : null)
                .aircraftName(seg.getAircraft() != null ? seg.getAircraft().getName() : null)
                .cabinClass(cabinClass)
                .cabinClassMarketingName(cabinClassMarketingName)
                .checkedBagsCount(checkedBagsCount)
                .technicalStopsCount(technicalStops)
                .build();
    }

    private int resolveCheckedBags(SegmentPassenger passenger) {
        if (passenger == null || passenger.getBaggages() == null) return 0;
        return passenger.getBaggages().stream()
                .filter(b -> "checked_bags".equals(b.getType()))
                .mapToInt(Baggage::getQuantity)
                .sum();
    }

    // ── Tag computation ──────────────────────────────────────────────────────

    private List<String> computeTags(String offerId, Set<String> cheapestIds,
                                     Set<String> fastestIds, Set<String> bestIds) {
        List<String> tags = new ArrayList<>();
        if (cheapestIds.contains(offerId)) tags.add("CHEAPEST");
        if (fastestIds.contains(offerId))  tags.add("FASTEST");
        if (bestIds.contains(offerId))     tags.add("BEST");
        return tags;
    }

    private <T extends Comparable<T>> Set<String> findMinValueIds(Map<String, T> valueById) {
        T min = valueById.values().stream().min(Comparator.naturalOrder()).orElse(null);
        if (min == null) return Set.of();
        return valueById.entrySet().stream()
                .filter(e -> e.getValue().compareTo(min) == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Set<String> computeBestIds(Map<String, BigDecimal> priceById, Map<String, Long> durationById) {
        if (priceById.isEmpty()) return Set.of();

        BigDecimal maxPrice  = priceById.values().stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
        long maxDuration     = durationById.values().stream().max(Long::compareTo).orElse(1L);

        Map<String, Double> scores = priceById.keySet().stream().collect(Collectors.toMap(
                id -> id,
                id -> {
                    double normPrice    = maxPrice.compareTo(BigDecimal.ZERO) == 0 ? 0
                            : priceById.get(id).doubleValue() / maxPrice.doubleValue();
                    double normDuration = maxDuration == 0 ? 0
                            : (double) durationById.getOrDefault(id, 0L) / maxDuration;
                    return 0.5 * normPrice + 0.5 * normDuration;
                }));

        double minScore = scores.values().stream().min(Double::compareTo).orElse(Double.MAX_VALUE);
        return scores.entrySet().stream()
                .filter(e -> Double.compare(e.getValue(), minScore) == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    // ── Duration helpers ─────────────────────────────────────────────────────

    private long totalDurationMinutes(List<OfferSlice> slices) {
        if (slices == null) return Long.MAX_VALUE;
        return slices.stream().mapToLong(s -> parseDurationMinutes(s.getDuration())).sum();
    }

    private long parseDurationMinutes(String isoDuration) {
        if (isoDuration == null || isoDuration.isBlank()) return 0;
        try {
            return Duration.parse(isoDuration).toMinutes();
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatDuration(String isoDuration) {
        if (isoDuration == null || isoDuration.isBlank()) return null;
        try {
            Duration d = Duration.parse(isoDuration);
            long hours   = d.toHours();
            long minutes = d.toMinutesPart();
            if (hours > 0 && minutes > 0) return hours + "h " + minutes + "m";
            if (hours > 0) return hours + "h";
            return minutes + "m";
        } catch (Exception e) {
            return isoDuration;
        }
    }

    private String computeLayoverDuration(String arrivingAt, String departingAt) {
        if (arrivingAt == null || departingAt == null) return null;
        try {
            Duration layover = Duration.between(LocalDateTime.parse(arrivingAt), LocalDateTime.parse(departingAt));
            return formatDuration(layover.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // ── Parsing helpers ──────────────────────────────────────────────────────

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        return new BigDecimal(value);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        return Instant.parse(value);
    }
}
