package com.shanmugapriya.notificationconsumer.service;

import com.shanmugapriya.notificationconsumer.event.OrderEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * NotificationService has no external dependencies to mock — these tests mainly
 * verify it handles every known event type (and unknown ones) without throwing,
 * since the real "assertion" here would be log output, which isn't unit-tested directly.
 * In a real system with a real email/SMS provider client, that client would be mocked
 * and verified here the same way OrderEventPublisher is verified in order-service.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private final NotificationService service = new NotificationService();

    @Test
    void handlesOrderPlacedEvent() {
        OrderEvent event = event("ORDER_PLACED");
        assertDoesNotThrow(() -> service.handle(event));
    }

    @Test
    void handlesOrderCancelledEvent() {
        OrderEvent event = event("ORDER_CANCELLED");
        assertDoesNotThrow(() -> service.handle(event));
    }

    @Test
    void handlesOrderShippedEvent() {
        OrderEvent event = event("ORDER_SHIPPED");
        assertDoesNotThrow(() -> service.handle(event));
    }

    @Test
    void handlesUnknownEventTypeGracefully() {
        OrderEvent event = event("SOME_FUTURE_EVENT_TYPE");
        // Should log a warning and return, never throw — an unrecognized event type
        // shouldn't crash the consumer or block the queue.
        assertDoesNotThrow(() -> service.handle(event));
    }

    private OrderEvent event(String type) {
        OrderEvent event = new OrderEvent();
        event.setEventType(type);
        event.setOrderId("order-1");
        event.setCustomerId("cust-1");
        event.setProductId("prod-1");
        event.setQuantity(2);
        event.setTotalAmount(new BigDecimal("998.00"));
        return event;
    }
}
