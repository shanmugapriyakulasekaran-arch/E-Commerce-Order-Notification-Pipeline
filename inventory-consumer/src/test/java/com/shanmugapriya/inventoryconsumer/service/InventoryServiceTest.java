package com.shanmugapriya.inventoryconsumer.service;

import com.shanmugapriya.inventoryconsumer.event.OrderEvent;
import com.shanmugapriya.inventoryconsumer.model.InventoryItem;
import com.shanmugapriya.inventoryconsumer.model.ProcessedEvent;
import com.shanmugapriya.inventoryconsumer.repository.InventoryRepository;
import com.shanmugapriya.inventoryconsumer.repository.ProcessedEventRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, processedEventRepository);
    }

    @Test
    void orderPlaced_decrementsStock() {
        OrderEvent event = event("ORDER_PLACED", "prod-1", 3);
        when(processedEventRepository.existsById(any())).thenReturn(false);
        when(inventoryRepository.findById("prod-1"))
                .thenReturn(Optional.of(new InventoryItem("prod-1", 10)));

        inventoryService.handle(event);

        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getStockQuantity());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void orderPlaced_createsInventoryRecordIfProductUnseen() {
        OrderEvent event = event("ORDER_PLACED", "new-product", 2);
        when(processedEventRepository.existsById(any())).thenReturn(false);
        when(inventoryRepository.findById("new-product")).thenReturn(Optional.empty());

        inventoryService.handle(event);

        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository).save(captor.capture());
        // starts at 0, decrement floors at 0 — documents current behavior for
        // "order placed for a product with no known stock record"
        assertEquals(0, captor.getValue().getStockQuantity());
    }

    @Test
    void orderCancelled_restoresStock() {
        OrderEvent event = event("ORDER_CANCELLED", "prod-1", 3);
        when(processedEventRepository.existsById(any())).thenReturn(false);
        when(inventoryRepository.findById("prod-1"))
                .thenReturn(Optional.of(new InventoryItem("prod-1", 7)));

        inventoryService.handle(event);

        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository).save(captor.capture());
        assertEquals(10, captor.getValue().getStockQuantity());
    }

    @Test
    void duplicateEventDelivery_isSkipped() {
        // This is THE critical test: proves at-least-once SQS redelivery doesn't
        // double-decrement stock.
        OrderEvent event = event("ORDER_PLACED", "prod-1", 3);
        when(processedEventRepository.existsById("order-1:ORDER_PLACED")).thenReturn(true);

        inventoryService.handle(event);

        verify(inventoryRepository, never()).save(any());
        verify(inventoryRepository, never()).findById(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void orderShipped_takesNoInventoryAction() {
        OrderEvent event = event("ORDER_SHIPPED", "prod-1", 3);
        when(processedEventRepository.existsById(any())).thenReturn(false);

        inventoryService.handle(event);

        verify(inventoryRepository, never()).save(any());
        // still marks as processed so a redelivered SHIPPED event is also a no-op, not reprocessed
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    private OrderEvent event(String type, String productId, int quantity) {
        OrderEvent event = new OrderEvent();
        event.setEventType(type);
        event.setOrderId("order-1");
        event.setCustomerId("cust-1");
        event.setProductId(productId);
        event.setQuantity(quantity);
        event.setTotalAmount(new BigDecimal("100.00"));
        return event;
    }
}
