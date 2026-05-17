package com.triptrekker.modules.audit.internal.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class AuditMessagingConfig {

    public static final String EXCHANGE = "triptrekker.integration.audit";
    public static final String QUEUE = "triptrekker.integration.audit.queue";
    public static final String ROUTING_KEY = "integration.audit";

    private static final String DLX = "triptrekker.integration.audit.dlx";
    private static final String DLQ = "triptrekker.integration.audit.dlq";
    private static final String DL_ROUTING_KEY = "integration.audit.dead";

    @Bean
    DirectExchange integrationAuditExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    Queue integrationAuditQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DL_ROUTING_KEY)
                .build();
    }

    @Bean
    DirectExchange integrationAuditDeadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    @Bean
    Queue integrationAuditDeadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding integrationAuditBinding() {
        return BindingBuilder.bind(integrationAuditQueue())
                .to(integrationAuditExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    Binding integrationAuditDeadLetterBinding() {
        return BindingBuilder.bind(integrationAuditDeadLetterQueue())
                .to(integrationAuditDeadLetterExchange())
                .with(DL_ROUTING_KEY);
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