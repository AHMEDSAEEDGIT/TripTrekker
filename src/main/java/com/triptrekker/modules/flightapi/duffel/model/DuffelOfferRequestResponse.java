package com.triptrekker.modules.flightapi.duffel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DuffelOfferRequestResponse {

    private Data data;

    @Getter
    @Setter
    public static class Data {
        private String id;
        private List<Offer> offers;
    }

    // ── Offer ────────────────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class Offer {
        private String id;

        @JsonProperty("total_amount")
        private String totalAmount;

        @JsonProperty("base_amount")
        private String baseAmount;

        @JsonProperty("tax_amount")
        private String taxAmount;

        @JsonProperty("total_currency")
        private String totalCurrency;

        @JsonProperty("base_currency")
        private String baseCurrency;

        @JsonProperty("tax_currency")
        private String taxCurrency;

        @JsonProperty("expires_at")
        private String expiresAt;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("total_emissions_kg")
        private String totalEmissionsKg;

        private OfferConditions conditions;
        private Airline owner;
        private List<OfferSlice> slices;
    }

    // ── Offer conditions (refund / change) ───────────────────────────────────

    @Getter
    @Setter
    public static class OfferConditions {
        @JsonProperty("refund_before_departure")
        private ConditionDetail refundBeforeDeparture;

        @JsonProperty("change_before_departure")
        private ConditionDetail changeBeforeDeparture;
    }

    @Getter
    @Setter
    public static class ConditionDetail {
        private Boolean allowed;

        @JsonProperty("penalty_amount")
        private String penaltyAmount;

        @JsonProperty("penalty_currency")
        private String penaltyCurrency;
    }

    // ── Airline ──────────────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class Airline {
        @JsonProperty("iata_code")
        private String iataCode;

        private String name;
    }

    // ── Slice ────────────────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class OfferSlice {
        private String id;
        private Place origin;
        private Place destination;
        private String duration;

        @JsonProperty("fare_brand_name")
        private String fareBrandName;

        private List<OfferSegment> segments;
    }

    // ── Airport / Place ──────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class Place {
        @JsonProperty("iata_code")
        private String iataCode;

        private String name;

        @JsonProperty("city_name")
        private String cityName;

        @JsonProperty("time_zone")
        private String timeZone;
    }

    // ── Segment ──────────────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class OfferSegment {
        private String id;
        private Place origin;
        private Place destination;

        @JsonProperty("origin_terminal")
        private String originTerminal;

        @JsonProperty("destination_terminal")
        private String destinationTerminal;

        @JsonProperty("departing_at")
        private String departingAt;

        @JsonProperty("arriving_at")
        private String arrivingAt;

        private String duration;
        private String distance;

        @JsonProperty("marketing_carrier")
        private Airline marketingCarrier;

        @JsonProperty("marketing_carrier_flight_number")
        private String marketingCarrierFlightNumber;

        @JsonProperty("operating_carrier")
        private Airline operatingCarrier;

        @JsonProperty("operating_carrier_flight_number")
        private String operatingCarrierFlightNumber;

        private Aircraft aircraft;
        private List<Stop> stops;
        private List<SegmentPassenger> passengers;
    }

    // ── Aircraft ─────────────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class Aircraft {
        private String id;

        @JsonProperty("iata_code")
        private String iataCode;

        private String name;
    }

    // ── Technical stop within a segment ─────────────────────────────────────

    @Getter
    @Setter
    public static class Stop {
        private String id;
        private Place airport;

        @JsonProperty("arriving_at")
        private String arrivingAt;

        @JsonProperty("departing_at")
        private String departingAt;

        private String duration;
    }

    // ── Passenger per segment ────────────────────────────────────────────────

    @Getter
    @Setter
    public static class SegmentPassenger {
        @JsonProperty("passenger_id")
        private String passengerId;

        @JsonProperty("cabin_class")
        private String cabinClass;

        @JsonProperty("cabin_class_marketing_name")
        private String cabinClassMarketingName;

        @JsonProperty("fare_basis_code")
        private String fareBasisCode;

        private List<Baggage> baggages;
    }

    // ── Baggage allowance ────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class Baggage {
        private String type;     // e.g. "checked_bags", "carry_on"
        private int quantity;
    }
}
