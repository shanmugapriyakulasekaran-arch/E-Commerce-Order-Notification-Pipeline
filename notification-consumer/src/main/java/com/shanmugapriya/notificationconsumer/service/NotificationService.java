package com.shanmugapriya.notificationconsumer.service;

import com.shanmugapriya.notificationconsumer.event.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Pure business logic — no SQS/AWS imports. Testable in isolation (see NotificationServiceTest).
 * The listener class is the only place that knows this runs off an SQS message.
 *
 * In a real system, "send" would call SES, Twilio, FCM, etc. Simulated here via logging
 * so the project can be demoed/tested without needing real notification provider credentials.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void handle(OrderEvent event) {
        switch (event.getEventType()) {
            case "ORDER_PLACED" -> sendOrderConfirmation(event);
            case "ORDER_CANCELLED" -> sendCancellationNotice(event);
            case "ORDER_SHIPPED" -> sendShippingNotice(event);
            default -> log.warn("Unknown event type '{}' for order {} — no notification sent",
                    event.getEventType(), event.getOrderId());
        }
    }

    private void sendOrderConfirmation(OrderEvent event) {
        log.info("[SIMULATED EMAIL] To customer {}: Your order {} for {} unit(s) (total ₹{}) has been placed!",
                event.getCustomerId(), event.getOrderId(), event.getQuantity(), event.getTotalAmount());
    }

    private void sendCancellationNotice(OrderEvent event) {
        log.info("[SIMULATED EMAIL] To customer {}: Your order {} has been cancelled.",
                event.getCustomerId(), event.getOrderId());
    }

    private void sendShippingNotice(OrderEvent event) {
        log.info("[SIMULATED SMS] To customer {}: Your order {} has shipped!",
                event.getCustomerId(), event.getOrderId());
    }
}
