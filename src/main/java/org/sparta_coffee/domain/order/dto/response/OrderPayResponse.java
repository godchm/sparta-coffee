package org.sparta_coffee.domain.order.dto.response;

import lombok.Builder;
import org.sparta_coffee.domain.order.entity.Order;

@Builder
public record OrderPayResponse(
        OrderResponse order,
        long remainingPoint
) {
    public static OrderPayResponse from(Order order, long remainingPoint) {
        return OrderPayResponse.builder()
                .order(OrderResponse.from(order))
                .remainingPoint(remainingPoint)
                .build();
    }
}