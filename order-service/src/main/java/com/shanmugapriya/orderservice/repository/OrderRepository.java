package com.shanmugapriya.orderservice.repository;

import com.shanmugapriya.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
}
