package com.shanmugapriya.orderservice.dto;

import com.shanmugapriya.orderservice.model.Order;
import com.shanmugapriya.orderservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderResponse {

    private String id;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Instant createdAt;

    public static OrderResponse from(Order order) {
        OrderResponse response = new OrderResponse();
        response.id = order.getId();
        response.customerId = order.getCustomerId();
        response.productId = order.getProductId();
        response.quantity = order.getQuantity();
        response.unitPrice = order.getUnitPrice();
        response.totalAmount = order.getTotalAmount();
        response.status = order.getStatus();
        response.createdAt = order.getCreatedAt();
        return response;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
