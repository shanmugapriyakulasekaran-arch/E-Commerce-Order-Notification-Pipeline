package com.shanmugapriya.analyticsconsumer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Simple running aggregate per event type, keyed by eventType.
 * A real analytics pipeline would likely stream into something like Kinesis + a data
 * warehouse rather than a single summary row per event type — this simplified version
 * demonstrates the consumption pattern without needing that infrastructure for a demo.
 */
@Entity
@Table(name = "event_summary")
public class EventSummary {

    @Id
    private String eventType;

    private Long eventCount;
    private BigDecimal totalAmountSum;

    protected EventSummary() {
        // JPA
    }

    public EventSummary(String eventType) {
        this.eventType = eventType;
        this.eventCount = 0L;
        this.totalAmountSum = BigDecimal.ZERO;
    }

    public void increment(BigDecimal amount) {
        this.eventCount++;
        this.totalAmountSum = this.totalAmountSum.add(amount == null ? BigDecimal.ZERO : amount);
    }

    public String getEventType() {
        return eventType;
    }

    public Long getEventCount() {
        return eventCount;
    }

    public BigDecimal getTotalAmountSum() {
        return totalAmountSum;
    }
}
