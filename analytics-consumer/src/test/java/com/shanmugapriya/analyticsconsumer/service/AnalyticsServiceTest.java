package com.shanmugapriya.analyticsconsumer.service;

import com.shanmugapriya.analyticsconsumer.event.OrderEvent;
import com.shanmugapriya.analyticsconsumer.model.EventSummary;
import com.shanmugapriya.analyticsconsumer.repository.EventSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private EventSummaryRepository repository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(repository);
    }

    @Test
    void firstEventOfType_createsNewSummary() {
        OrderEvent event = event("ORDER_PLACED", "500.00");
        when(repository.findById("ORDER_PLACED")).thenReturn(Optional.empty());

        analyticsService.handle(event);

        ArgumentCaptor<EventSummary> captor = ArgumentCaptor.forClass(EventSummary.class);
        verify(repository).save(captor.capture());
        assertEquals(1L, captor.getValue().getEventCount());
        assertEquals(new BigDecimal("500.00"), captor.getValue().getTotalAmountSum());
    }

    @Test
    void subsequentEventOfSameType_incrementsExistingSummary() {
        EventSummary existing = new EventSummary("ORDER_PLACED");
        existing.increment(new BigDecimal("200.00")); // simulate one prior event
        when(repository.findById("ORDER_PLACED")).thenReturn(Optional.of(existing));

        OrderEvent event = event("ORDER_PLACED", "300.00");
        analyticsService.handle(event);

        ArgumentCaptor<EventSummary> captor = ArgumentCaptor.forClass(EventSummary.class);
        verify(repository).save(captor.capture());
        assertEquals(2L, captor.getValue().getEventCount());
        assertEquals(new BigDecimal("500.00"), captor.getValue().getTotalAmountSum());
    }

    @Test
    void differentEventTypes_trackedSeparately() {
        when(repository.findById("ORDER_CANCELLED")).thenReturn(Optional.empty());

        OrderEvent event = event("ORDER_CANCELLED", "150.00");
        analyticsService.handle(event);

        verify(repository).findById("ORDER_CANCELLED");
        verify(repository, org.mockito.Mockito.never()).findById("ORDER_PLACED");
    }

    private OrderEvent event(String type, String amount) {
        OrderEvent event = new OrderEvent();
        event.setEventType(type);
        event.setOrderId("order-1");
        event.setCustomerId("cust-1");
        event.setProductId("prod-1");
        event.setQuantity(1);
        event.setTotalAmount(new BigDecimal(amount));
        return event;
    }
}
