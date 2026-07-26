package com.shanmugapriya.inventoryconsumer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory")
public class InventoryItem {

    @Id
    private String productId;

    private Integer stockQuantity;

    protected InventoryItem() {
        // JPA
    }

    public InventoryItem(String productId, Integer stockQuantity) {
        this.productId = productId;
        this.stockQuantity = stockQuantity;
    }

    public void decrement(int amount) {
        this.stockQuantity = Math.max(0, this.stockQuantity - amount);
    }

    public String getProductId() {
        return productId;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }
}
