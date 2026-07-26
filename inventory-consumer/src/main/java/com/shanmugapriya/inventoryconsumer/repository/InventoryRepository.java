package com.shanmugapriya.inventoryconsumer.repository;

import com.shanmugapriya.inventoryconsumer.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {
}
