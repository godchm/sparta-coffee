package org.sparta_coffee.common.config.model.kafka.event;

import java.time.LocalDateTime;


public record PopularRankingEvent(
        String keyword,
        Long userId,
        LocalDateTime searchedAt
) {
}