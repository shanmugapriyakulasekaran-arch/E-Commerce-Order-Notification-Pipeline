package com.shanmugapriya.orderservice.service;

import com.shanmugapriya.orderservice.dto.PlaceOrderRequest;
import com.shanmugapriya.orderservice.event.OrderEvent;
import com.shanmugapriya.orderservice.event.OrderEventPublisher;
import com.shanmugapriya.orderservice.exception.InvalidOrderStateException;
import com.shanmugapriya.orderservice.exception.OrderNotFoundException;
import com.shanmugapriya.orderservice.model.Order;
import com.shanmugapriya.orderservice.model.OrderStatus;
import com.shanmugapriya.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests OrderService with BOTH OrderRepository and OrderEventPublisher mocked.
 * No database, no AWS/SNS, no network — this whole test class runs in milliseconds
 * and verifies the actual business rules (state transitions, event publishing order).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, eventPublisher);
    }

    @Test
    void placeOrder_savesAndPublishesEvent() {
        PlaceOrderRequest request = validRequest();
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.placeOrder(request);

        assertEquals(OrderStatus.PLACED, result.getStatus());
        assertEquals(new BigDecimal("998.00"), result.getTotalAmount());

        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        assertEquals("ORDER_PLACED", eventCaptor.getValue().getEventType());
    }

    @Test
    void placeOrder_publishesEventOnlyAfterSaveSucceeds() {
        PlaceOrderRequest request = validRequest();
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("DB down"));

        assertThrows(RuntimeException.class, () -> orderService.placeOrder(request));

        // Critical business rule: never notify downstream systems about an order
        // that didn't actually persist.
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void cancelOrder_transitionsFromPlacedToCancelled() {
        Order existing = new Order("cust-1", "prod-1", 2, new BigDecimal("100.00"));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancelOrder("order-1");

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(eventPublisher).publish(argThat(e -> e.getEventType().equals("ORDER_CANCELLED")));
    }

    @Test
    void cancelOrder_throwsWhenAlreadyShipped() {
        Order existing = new Order("cust-1", "prod-1", 1, new BigDecimal("50.00"));
        existing.markShipped();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(existing));

        assertThrows(InvalidOrderStateException.class, () -> orderService.cancelOrder("order-1"));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void cancelOrder_throwsWhenOrderDoesNotExist() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder("missing"));
    }

    @Test
    void markShipped_onlyAllowedFromPlacedStatus() {
        Order existing = new Order("cust-1", "prod-1", 1, new BigDecimal("50.00"));
        existing.markCancelled();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(existing));

        assertThrows(InvalidOrderStateException.class, () -> orderService.markShipped("order-1"));
    }

    @Test
    void markShipped_publishesShippedEvent() {
        Order existing = new Order("cust-1", "prod-1", 1, new BigDecimal("50.00"));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.markShipped("order-1");

        verify(eventPublisher).publish(argThat(e -> e.getEventType().equals("ORDER_SHIPPED")));
    }

    private PlaceOrderRequest validRequest() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCustomerId("cust-1");
        request.setProductId("prod-1");
        request.setQuantity(2);
        request.setUnitPrice(new BigDecimal("499.00"));
        return request;
    }
}
