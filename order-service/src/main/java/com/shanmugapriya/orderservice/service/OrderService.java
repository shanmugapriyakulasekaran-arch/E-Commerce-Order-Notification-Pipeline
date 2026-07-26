package com.shanmugapriya.orderservice.service;

import com.shanmugapriya.orderservice.dto.PlaceOrderRequest;
import com.shanmugapriya.orderservice.event.OrderEvent;
import com.shanmugapriya.orderservice.event.OrderEventPublisher;
import com.shanmugapriya.orderservice.exception.InvalidOrderStateException;
import com.shanmugapriya.orderservice.exception.OrderNotFoundException;
import com.shanmugapriya.orderservice.model.Order;
import com.shanmugapriya.orderservice.model.OrderStatus;
import com.shanmugapriya.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic only. Depends on OrderEventPublisher as a plain collaborator —
 * see OrderServiceTest for how this is unit tested with both dependencies mocked,
 * with no database and no AWS involved.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order placeOrder(PlaceOrderRequest request) {
        Order order = new Order(
                request.getCustomerId(),
                request.getProductId(),
                request.getQuantity(),
                request.getUnitPrice()
        );

        Order saved = orderRepository.save(order);

        // Publish AFTER the DB commit succeeds — we never want to notify downstream
        // systems about an order that didn't actually persist.
        eventPublisher.publish(OrderEvent.placed(saved));

        return saved;
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public Order cancelOrder(String orderId) {
        Order order = getOrder(orderId);

        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new InvalidOrderStateException("Cannot cancel an order that has already shipped: " + orderId);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order is already cancelled: " + orderId);
        }

        order.markCancelled();
        Order saved = orderRepository.save(order);

        eventPublisher.publish(OrderEvent.cancelled(saved));

        return saved;
    }

    @Transactional
    public Order markShipped(String orderId) {
        Order order = getOrder(orderId);

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new InvalidOrderStateException(
                    "Only PLACED orders can be marked shipped. Current status: " + order.getStatus());
        }

        order.markShipped();
        Order saved = orderRepository.save(order);

        eventPublisher.publish(OrderEvent.shipped(saved));

        return saved;
    }
}
