package com.shanmugapriya.orderservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.awspring.cloud.sns.core.SnsTemplate;

/**
 * The ONLY class in this service that knows about SNS.
 * OrderService (business logic) depends on this as a plain collaborator —
 * it doesn't know or care that "publish" means "call AWS SNS" under the hood.
 * This isolation is what makes OrderService unit-testable with a mocked publisher.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final SnsTemplate snsTemplate;
    private final ObjectMapper objectMapper;
    private final String topicArn;

    public OrderEventPublisher(SnsTemplate snsTemplate,
                                ObjectMapper objectMapper,
                                @Value("${app.sns.order-events-topic-arn}") String topicArn) {
        this.snsTemplate = snsTemplate;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

    public void publish(OrderEvent event) {
        try {
            snsTemplate.convertAndSend(topicArn, event);
            log.info("Published {} event for order {}", event.getEventType(), event.getOrderId());
        } catch (Exception e) {
            // In production this would also increment a CloudWatch metric and/or
            // write to an outbox table for retry — logging here for demo clarity.
            log.error("Failed to publish {} event for order {}: {}",
                    event.getEventType(), event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }
}
