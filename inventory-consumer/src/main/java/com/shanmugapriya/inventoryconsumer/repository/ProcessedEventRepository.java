package com.shanmugapriya.inventoryconsumer.repository;

import com.shanmugapriya.inventoryconsumer.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
