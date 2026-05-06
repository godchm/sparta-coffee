package org.sparta_coffee.domain.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.sparta_coffee.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {


}
