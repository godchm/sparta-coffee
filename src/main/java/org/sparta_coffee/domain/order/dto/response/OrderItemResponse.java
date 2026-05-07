package org.sparta_coffee.domain.order.dto.response;

import lombok.Builder;
import org.sparta_coffee.domain.order.entity.OrderItem;

@Builder
public record OrderItemResponse(
        Long menuId,
        String menuName,
        long menuPrice,
        int quantity,
        long subtotalAmount
) {
    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .menuId(item.getMenuId())
                .menuName(item.getMenuName())
                .menuPrice(item.getMenuPrice())
                .quantity(item.getQuantity())
                .subtotalAmount(item.getSubtotalAmount())
                .build();
    }
}