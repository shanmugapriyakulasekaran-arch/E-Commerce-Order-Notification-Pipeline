package com.shanmugapriya.inventoryconsumer.listener;

import com.shanmugapriya.inventoryconsumer.event.OrderEvent;
import com.shanmugapriya.inventoryconsumer.service.InventoryService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryListener.class);

    private final InventoryService inventoryService;

    public InventoryListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @SqsListener("${app.sqs.inventory-queue-name}")
    public void onOrderEvent(OrderEvent event) {
        log.debug("Received event {} for order {}", event.getEventType(), event.getOrderId());
        inventoryService.handle(event);
    }
}
