package com.triptrekker.modules.flightapi.duffel.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "flight.providers.duffel")
public class DuffelProperties {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String apiToken;
}
