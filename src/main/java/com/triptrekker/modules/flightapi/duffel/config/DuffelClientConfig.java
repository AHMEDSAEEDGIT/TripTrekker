package com.triptrekker.modules.flightapi.duffel.config;

import com.triptrekker.modules.audit.api.AuditingRestClientInterceptor;
import com.triptrekker.modules.audit.api.IntegrationAuditPublisher;
import com.triptrekker.modules.audit.api.IntegrationVendor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class DuffelClientConfig {

    private final DuffelProperties duffelProperties;
    private final IntegrationAuditPublisher auditPublisher;

    @Bean("duffelRestClient")
    public RestClient duffelRestClient() {
        return RestClient.builder()
                .baseUrl(duffelProperties.getBaseUrl())
                .requestInterceptor(new AuditingRestClientInterceptor(IntegrationVendor.DUFFEL, auditPublisher))
                .build();
    }
}
