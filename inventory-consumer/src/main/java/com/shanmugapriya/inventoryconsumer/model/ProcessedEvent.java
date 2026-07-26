package com.shanmugapriya.inventoryconsumer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Tracks which order events have already been processed, keyed by orderId + eventType.
 *
 * WHY THIS EXISTS: SQS guarantees at-least-once delivery, meaning the SAME message can
 * be delivered more than once (e.g. if the consumer crashes after decrementing stock but
 * before the message is deleted from the queue). Without this table, a redelivered
 * "ORDER_PLACED" event would decrement stock twice for one real order.
 *
 * This is the idempotency mechanism referenced in the root README's Design Decisions section.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    private String eventKey; // format: "{orderId}:{eventType}"

    private Instant processedAt;

    protected ProcessedEvent() {
        // JPA
    }

    public ProcessedEvent(String orderId, String eventType) {
        this.eventKey = buildKey(orderId, eventType);
        this.processedAt = Instant.now();
    }

    public static String buildKey(String orderId, String eventType) {
        return orderId + ":" + eventType;
    }

    public String getEventKey() {
        return eventKey;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
