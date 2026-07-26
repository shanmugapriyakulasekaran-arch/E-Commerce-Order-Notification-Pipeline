package com.shanmugapriya.notificationconsumer.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Deserialization target for the shared OrderEvent contract published by order-service.
 *
 * Deliberately duplicated here rather than shared via a common library module —
 * in a real microservices setup each team/service typically owns its own copy of the
 * consumed event shape (sometimes via a schema registry/Avro instead of a shared JAR),
 * so that Order Service can evolve its internal model without forcing every consumer
 * to redeploy in lockstep. See root README's "contract testing" note for the trade-off
 * this introduces (schema drift risk).
 */
public class OrderEvent {

    private String eventType;
    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Instant occurredAt;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
