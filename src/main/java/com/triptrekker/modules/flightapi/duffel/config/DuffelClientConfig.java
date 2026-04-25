package com.triptrekker.modules.flightapi.duffel.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class DuffelClientConfig {

    private final DuffelProperties duffelProperties;

    @Bean("duffelRestClient")
    public RestClient duffelRestClient() {
        return RestClient.builder()
                .baseUrl(duffelProperties.getBaseUrl())
                .build();
    }
}
