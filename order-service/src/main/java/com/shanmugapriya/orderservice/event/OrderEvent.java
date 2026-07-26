package com.shanmugapriya.orderservice.event;

import com.shanmugapriya.orderservice.model.Order;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The event contract published to the SNS "order-events" topic.
 *
 * This is intentionally a flat, self-contained payload (not just an order ID) —
 * consumers can act on it without needing to call back into Order Service to fetch
 * details, which keeps services decoupled and avoids a "fan back in" dependency.
 *
 * IMPORTANT: this shape is a contract shared across 4 services. If you change a field
 * here, the notification/inventory/analytics consumers' deserialization must be updated
 * too — see the "What I'd add next" note in the root README about contract testing.
 */
public class OrderEvent {

    private String eventType;      // ORDER_PLACED | ORDER_CANCELLED | ORDER_SHIPPED
    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Instant occurredAt;

    public OrderEvent() {
    }

    public static OrderEvent placed(Order order) {
        return build("ORDER_PLACED", order);
    }

    public static OrderEvent cancelled(Order order) {
        return build("ORDER_CANCELLED", order);
    }

    public static OrderEvent shipped(Order order) {
        return build("ORDER_SHIPPED", order);
    }

    private static OrderEvent build(String eventType, Order order) {
        OrderEvent event = new OrderEvent();
        event.eventType = eventType;
        event.orderId = order.getId();
        event.customerId = order.getCustomerId();
        event.productId = order.getProductId();
        event.quantity = order.getQuantity();
        event.totalAmount = order.getTotalAmount();
        event.occurredAt = Instant.now();
        return event;
    }

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
