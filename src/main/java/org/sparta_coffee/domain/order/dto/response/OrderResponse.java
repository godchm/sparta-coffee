package org.sparta_coffee.domain.order.dto.response;


import lombok.Builder;
import org.sparta_coffee.domain.order.entity.Order;
import org.sparta_coffee.domain.order.entity.OrderItem;

import java.util.List;

@Builder
public record OrderResponse(
        Long orderId,
        Long userId,
        long paymentAmount,
        String status,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order, List<OrderItem> items) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .paymentAmount(order.getPaymentAmount())
                .status(order.getStatus().name())
                .items(items.stream()
                        .map(OrderItemResponse::from)
                        .toList())
                .build();
    }
}
