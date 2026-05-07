package org.sparta_coffee.common.config.model.kafka.event;

import java.time.LocalDateTime;

public record PopularRankingEvent(
        Long menuId,
        String menuName,
        long menuPrice,
        Long userId,
        long paymentAmount,
        LocalDateTime orderedAt,
        int quantity
) {
}