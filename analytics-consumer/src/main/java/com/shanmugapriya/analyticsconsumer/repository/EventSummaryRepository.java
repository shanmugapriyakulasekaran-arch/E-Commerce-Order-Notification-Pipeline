package com.shanmugapriya.analyticsconsumer.repository;

import com.shanmugapriya.analyticsconsumer.model.EventSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSummaryRepository extends JpaRepository<EventSummary, String> {
}
