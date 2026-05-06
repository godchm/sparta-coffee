package org.sparta_coffee.domain.popularRanking.producer;


import lombok.RequiredArgsConstructor;

import org.sparta_coffee.common.config.model.kafka.event.PopularRankingEvent;
import org.sparta_coffee.common.config.model.kafka.topic.PopularRankingTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PopularRankingProducer {

    private final KafkaTemplate<String, PopularRankingEvent> popularRankingKafkaTemplate;

    public void send(PopularRankingEvent event) {
        popularRankingKafkaTemplate.send(
                PopularRankingTopics.POPULAR_RANKING_EVENTS,
                event.keyword(),
                event
        );
    }
}