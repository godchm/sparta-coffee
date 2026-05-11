package org.sparta_coffee.domain.popularRanking.listener;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta_coffee.common.config.model.kafka.event.PopularRankingEvent;
import org.sparta_coffee.common.config.model.kafka.topic.PopularRankingTopics;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;


/**
 * Kafka로 전달된 인기 메뉴 집계 이벤트를 소비하는 Listener.
 *
 * 주문 결제 성공 후 발행된 PopularRankingEvent를 받아
 * Redis ZSET에 날짜별 메뉴 판매 수량을 누적.
 *
 * Redis ZSET 구조:
 * - Key: popular:menu:{yyyy-MM-dd}
 * - Value: menuId
 * - Score: 주문 수량
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularRankingListener {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 인기 메뉴 집계 Kafka 이벤트를 소비.
     *
     * 이벤트에 포함된 주문 일자를 기준으로 일별 Redis 랭킹 key를 생성하고,
     * 해당 메뉴 ID의 score를 주문 수량만큼 증가.
     *
     * @param event 주문 결제 후 생성된 인기 메뉴 집계 이벤트
     */
    @KafkaListener(
            topics = PopularRankingTopics.POPULAR_RANKING_EVENTS,
            groupId = "popular-ranking-group",
            containerFactory = "popularRankingKafkaListenerContainerFactory"
    )
    public void consume(PopularRankingEvent event) {
        String key = createDailyRankingKey(event.orderedAt().toLocalDate());

        stringRedisTemplate.opsForZSet()
                .incrementScore(key, String.valueOf(event.menuId()), event.quantity());
    }

    /**
     * 특정 날짜의 인기 메뉴 랭킹 Redis key를 생성.
     *
     * @param date 랭킹을 집계할 날짜
     * @return 날짜별 인기 메뉴 랭킹 key
     */
    private String createDailyRankingKey(LocalDate date) {
        return "popular:menu:" + date;
    }
}
