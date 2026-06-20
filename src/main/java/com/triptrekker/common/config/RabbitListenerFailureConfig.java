package com.triptrekker.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitListenerFailureConfig {

    @Bean
    MessageRecoverer loggingRejectAndDontRequeueRecoverer() {
        return new LoggingRejectAndDontRequeueRecoverer();
    }

    private static final class LoggingRejectAndDontRequeueRecoverer implements MessageRecoverer {

        @Override
        public void recover(Message message, Throwable cause) {
            MessageProperties properties = message.getMessageProperties();
            log.error(
                    "RabbitMQ listener retries exhausted; rejecting message without requeue. "
                            + "exchange={} routingKey={} consumerQueue={} messageId={} correlationId={}",
                    properties.getReceivedExchange(),
                    properties.getReceivedRoutingKey(),
                    properties.getConsumerQueue(),
                    properties.getMessageId(),
                    properties.getCorrelationId(),
                    cause
            );
            throw new ListenerExecutionFailedException(
                    "RabbitMQ listener retries exhausted",
                    new AmqpRejectAndDontRequeueException(cause),
                    message
            );
        }
    }
}
