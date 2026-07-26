package com.shanmugapriya.analyticsconsumer.listener;

import com.shanmugapriya.analyticsconsumer.event.OrderEvent;
import com.shanmugapriya.analyticsconsumer.service.AnalyticsService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsListener.class);

    private final AnalyticsService analyticsService;

    public AnalyticsListener(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @SqsListener("${app.sqs.analytics-queue-name}")
    public void onOrderEvent(OrderEvent event) {
        log.debug("Received event {} for order {}", event.getEventType(), event.getOrderId());
        analyticsService.handle(event);
    }
}
