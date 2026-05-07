package org.sparta_coffee.domain.order.repository;

import org.sparta_coffee.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrderId(Long orderId);

    void deleteAllByOrderId(Long orderId);
}