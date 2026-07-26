package com.shanmugapriya.analyticsconsumer.service;

import com.shanmugapriya.analyticsconsumer.event.OrderEvent;
import com.shanmugapriya.analyticsconsumer.model.EventSummary;
import com.shanmugapriya.analyticsconsumer.repository.EventSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pure business logic, no AWS/SQS imports — see AnalyticsServiceTest.
 *
 * Note: unlike InventoryService, this one is NOT strictly idempotency-guarded, since a
 * duplicate delivery here just slightly over-counts an analytics number rather than
 * corrupting real business state (like double-decrementing stock). This is a deliberate,
 * documented trade-off: not every consumer needs the same rigor — analytics tolerates
 * approximate counts, inventory does not.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final EventSummaryRepository repository;

    public AnalyticsService(EventSummaryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void handle(OrderEvent event) {
        EventSummary summary = repository.findById(event.getEventType())
                .orElseGet(() -> new EventSummary(event.getEventType()));

        summary.increment(event.getTotalAmount());
        repository.save(summary);

        log.info("Updated summary for {}: count={}, totalAmount={}",
                event.getEventType(), summary.getEventCount(), summary.getTotalAmountSum());
    }
}
