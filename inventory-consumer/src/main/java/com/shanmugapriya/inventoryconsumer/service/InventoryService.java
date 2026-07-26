package com.shanmugapriya.inventoryconsumer.service;

import com.shanmugapriya.inventoryconsumer.event.OrderEvent;
import com.shanmugapriya.inventoryconsumer.model.InventoryItem;
import com.shanmugapriya.inventoryconsumer.model.ProcessedEvent;
import com.shanmugapriya.inventoryconsumer.repository.InventoryRepository;
import com.shanmugapriya.inventoryconsumer.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pure business logic, no AWS/SQS imports — testable with mocked repositories (see
 * InventoryServiceTest). Only ORDER_PLACED events decrement stock; ORDER_CANCELLED
 * events restore it. ORDER_SHIPPED is ignored here (stock was already committed at
 * placement time).
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                             ProcessedEventRepository processedEventRepository) {
        this.inventoryRepository = inventoryRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void handle(OrderEvent event) {
        String eventKey = ProcessedEvent.buildKey(event.getOrderId(), event.getEventType());

        // Idempotency guard: SQS may redeliver the same message. If we've already
        // processed this exact (orderId, eventType) pair, skip it silently rather
        // than double-decrementing stock.
        if (processedEventRepository.existsById(eventKey)) {
            log.info("Event {} already processed — skipping duplicate delivery", eventKey);
            return;
        }

        switch (event.getEventType()) {
            case "ORDER_PLACED" -> decrementStock(event);
            case "ORDER_CANCELLED" -> restoreStock(event);
            case "ORDER_SHIPPED" -> log.debug("No inventory action needed for ORDER_SHIPPED");
            default -> log.warn("Unknown event type '{}' — no inventory action taken", event.getEventType());
        }

        processedEventRepository.save(new ProcessedEvent(event.getOrderId(), event.getEventType()));
    }

    private void decrementStock(OrderEvent event) {
        InventoryItem item = inventoryRepository.findById(event.getProductId())
                .orElseGet(() -> new InventoryItem(event.getProductId(), 0));

        item.decrement(event.getQuantity());
        inventoryRepository.save(item);

        log.info("Decremented stock for product {} by {} (order {})",
                event.getProductId(), event.getQuantity(), event.getOrderId());
    }

    private void restoreStock(OrderEvent event) {
        InventoryItem item = inventoryRepository.findById(event.getProductId())
                .orElseGet(() -> new InventoryItem(event.getProductId(), 0));

        item.decrement(-event.getQuantity()); // negative decrement = restore
        inventoryRepository.save(item);

        log.info("Restored stock for product {} by {} (order {} cancelled)",
                event.getProductId(), event.getQuantity(), event.getOrderId());
    }
}
