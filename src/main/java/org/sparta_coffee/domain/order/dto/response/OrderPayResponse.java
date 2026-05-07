package org.sparta_coffee.domain.order.dto.response;

import lombok.Builder;
import org.sparta_coffee.domain.order.entity.Order;
import org.sparta_coffee.domain.order.entity.OrderItem;

import java.util.List;

@Builder
public record OrderPayResponse(
        OrderResponse order,
        long remainingPoint
) {
    public static OrderPayResponse from(Order order, List<OrderItem> items, long remainingPoint) {
        return OrderPayResponse.builder()
                .order(OrderResponse.from(order, items))
                .remainingPoint(remainingPoint)
                .build();
    }
}
