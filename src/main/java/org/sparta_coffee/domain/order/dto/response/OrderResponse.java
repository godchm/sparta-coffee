package org.sparta_coffee.domain.order.dto.response;


import lombok.Builder;
import org.sparta_coffee.domain.order.entity.Order;

@Builder
public record OrderResponse(
        Long orderId,
        Long userId,
        Long menuId,
        String menuName,
        long paymentAmount,
        String status
) {

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .menuId(order.getMenu().getId())
                .menuName(order.getMenu().getName())
                .paymentAmount(order.getPaymentAmount())
                .status(order.getStatus().name())
                .build();
    }
}
