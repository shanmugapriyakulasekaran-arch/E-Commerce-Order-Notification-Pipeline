package com.shanmugapriya.notificationconsumer.listener;

import com.shanmugapriya.notificationconsumer.event.OrderEvent;
import com.shanmugapriya.notificationconsumer.service.NotificationService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The only class in this service that knows about SQS.
 * Spring Cloud AWS handles polling, deserialization (JSON -> OrderEvent), and — critically —
 * automatic deletion of the message from the queue only after this method returns successfully.
 * If this method throws, the message becomes visible again for retry, and after the queue's
 * configured max-receive-count, it lands in the dead-letter queue (see infra/aws-setup.md).
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @SqsListener("${app.sqs.notification-queue-name}")
    public void onOrderEvent(OrderEvent event) {
        log.debug("Received event {} for order {}", event.getEventType(), event.getOrderId());
        notificationService.handle(event);
    }
}
