package com.triptrekker.modules.notification.internal.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationRabbitMqConfig {

    public static final String NOTIFICATION_EXCHANGE = "triptrekker.notifications";
    public static final String NOTIFICATION_DLX = "triptrekker.notifications.dlx";

    public static final String EMAIL_QUEUE = "triptrekker.notifications.email.queue";
    public static final String EMAIL_ROUTING_KEY = "notification.email";
    public static final String EMAIL_DLQ = "triptrekker.notifications.email.dlq";
    public static final String EMAIL_DL_ROUTING_KEY = "notification.email.dead";

    public static final String SMS_QUEUE = "triptrekker.notifications.sms.queue";
    public static final String SMS_ROUTING_KEY = "notification.sms";
    public static final String SMS_DLQ = "triptrekker.notifications.sms.dlq";
    public static final String SMS_DL_ROUTING_KEY = "notification.sms.dead";

    public static final String SSE_QUEUE = "triptrekker.notifications.sse.queue";
    public static final String SSE_ROUTING_KEY = "notification.sse";
    public static final String SSE_DLQ = "triptrekker.notifications.sse.dlq";
    public static final String SSE_DL_ROUTING_KEY = "notification.sse.dead";

    @Bean
    TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange(NOTIFICATION_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange notificationDeadLetterExchange() {
        return ExchangeBuilder.directExchange(NOTIFICATION_DLX).durable(true).build();
    }

    @Bean
    Queue emailQueue() {
        return durableQueueWithDeadLetter(EMAIL_QUEUE, EMAIL_DL_ROUTING_KEY);
    }

    @Bean
    Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(notificationExchange()).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    Binding emailDeadLetterBinding() {
        return BindingBuilder.bind(emailDeadLetterQueue())
                .to(notificationDeadLetterExchange())
                .with(EMAIL_DL_ROUTING_KEY);
    }

    @Bean
    Queue smsQueue() {
        return durableQueueWithDeadLetter(SMS_QUEUE, SMS_DL_ROUTING_KEY);
    }

    @Bean
    Queue smsDeadLetterQueue() {
        return QueueBuilder.durable(SMS_DLQ).build();
    }

    @Bean
    Binding smsBinding() {
        return BindingBuilder.bind(smsQueue()).to(notificationExchange()).with(SMS_ROUTING_KEY);
    }

    @Bean
    Binding smsDeadLetterBinding() {
        return BindingBuilder.bind(smsDeadLetterQueue())
                .to(notificationDeadLetterExchange())
                .with(SMS_DL_ROUTING_KEY);
    }

    @Bean
    Queue sseQueue() {
        return durableQueueWithDeadLetter(SSE_QUEUE, SSE_DL_ROUTING_KEY);
    }

    @Bean
    Queue sseDeadLetterQueue() {
        return QueueBuilder.durable(SSE_DLQ).build();
    }

    @Bean
    Binding sseBinding() {
        return BindingBuilder.bind(sseQueue()).to(notificationExchange()).with(SSE_ROUTING_KEY);
    }

    @Bean
    Binding sseDeadLetterBinding() {
        return BindingBuilder.bind(sseDeadLetterQueue())
                .to(notificationDeadLetterExchange())
                .with(SSE_DL_ROUTING_KEY);
    }

    @Bean
    RestClient httpSmsRestClient() {
        return RestClient.builder().build();
    }

    private Queue durableQueueWithDeadLetter(String queue, String deadLetterRoutingKey) {
        return QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
                .build();
    }
}
