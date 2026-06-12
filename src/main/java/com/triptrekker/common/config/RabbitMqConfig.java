package com.triptrekker.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMqConfig {

    public static final String INTEGRATION_AUDIT_EXCHANGE = "triptrekker.integration.audit";
    public static final String INTEGRATION_AUDIT_QUEUE = "triptrekker.integration.audit.queue";
    public static final String INTEGRATION_AUDIT_ROUTING_KEY = "integration.audit";

    private static final String INTEGRATION_AUDIT_DLX = "triptrekker.integration.audit.dlx";
    private static final String INTEGRATION_AUDIT_DLQ = "triptrekker.integration.audit.dlq";
    private static final String INTEGRATION_AUDIT_DL_ROUTING_KEY = "integration.audit.dead";

    @Bean
    DirectExchange integrationAuditExchange() {
        return ExchangeBuilder.directExchange(INTEGRATION_AUDIT_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue integrationAuditQueue() {
        return QueueBuilder.durable(INTEGRATION_AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", INTEGRATION_AUDIT_DLX)
                .withArgument("x-dead-letter-routing-key", INTEGRATION_AUDIT_DL_ROUTING_KEY)
                .build();
    }

    @Bean
    DirectExchange integrationAuditDeadLetterExchange() {
        return ExchangeBuilder.directExchange(INTEGRATION_AUDIT_DLX).durable(true).build();
    }

    @Bean
    Queue integrationAuditDeadLetterQueue() {
        return QueueBuilder.durable(INTEGRATION_AUDIT_DLQ).build();
    }

    @Bean
    Binding integrationAuditBinding() {
        return BindingBuilder.bind(integrationAuditQueue())
                .to(integrationAuditExchange())
                .with(INTEGRATION_AUDIT_ROUTING_KEY);
    }

    @Bean
    Binding integrationAuditDeadLetterBinding() {
        return BindingBuilder.bind(integrationAuditDeadLetterQueue())
                .to(integrationAuditDeadLetterExchange())
                .with(INTEGRATION_AUDIT_DL_ROUTING_KEY);
    }

    /**
     * Configures JSON serialization for all RabbitMQ messages application-wide.
     * Spring Boot's autoconfigured RabbitTemplate and listener container factory
     * both pick this up automatically.
     */
    @Bean
    JacksonJsonMessageConverter jacksonMessageConverter() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
