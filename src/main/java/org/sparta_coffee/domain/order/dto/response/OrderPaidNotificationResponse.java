package org.sparta_coffee.domain.order.dto.response;


import lombok.Builder;

@Builder
public record OrderPaidNotificationResponse(
        String type,
        Long orderId,
        Long userId,
        Long menuId,
        String menuName,
        long paymentAmount,
        String message
) {
}
