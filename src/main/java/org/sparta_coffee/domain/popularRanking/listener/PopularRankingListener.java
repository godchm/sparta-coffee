package org.sparta_coffee.domain.popularRanking.listener;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta_coffee.common.config.model.kafka.event.PopularRankingEvent;
import org.sparta_coffee.common.config.model.kafka.topic.PopularRankingTopics;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularRankingListener {

    private final StringRedisTemplate stringRedisTemplate;

    @KafkaListener(
            topics = PopularRankingTopics.POPULAR_RANKING_EVENTS,
            groupId = "popular-ranking-group",
            containerFactory = "popularRankingKafkaListenerContainerFactory"
    )
    public void consume(PopularRankingEvent event) {
        String key = createDailyRankingKey(event.searchedAt().toLocalDate());

        stringRedisTemplate.opsForZSet()
                .incrementScore(key, event.keyword(), 1);
    }

    private String createDailyRankingKey(LocalDate date) {
        return "popular:ranking:" + date;
    }
}
