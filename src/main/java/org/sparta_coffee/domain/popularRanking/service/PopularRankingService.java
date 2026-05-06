package org.sparta_coffee.domain.popularRanking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

import org.sparta_coffee.common.config.model.kafka.event.PopularRankingEvent;
import org.sparta_coffee.common.config.model.redis.RankingDto;
import org.sparta_coffee.domain.menu.repository.MenuRepository;
import org.sparta_coffee.domain.popularRanking.dto.request.PopularRankingRequest;
import org.sparta_coffee.domain.popularRanking.producer.PopularRankingProducer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class PopularRankingService {

    private final PopularRankingProducer popularRankingProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final MenuRepository menuRepository;


    @Transactional
    public void search(PopularRankingRequest request, Long userId) {
        PopularRankingEvent event = new PopularRankingEvent(
                request.keyword(),
                userId,
                LocalDateTime.now()
        );

        popularRankingProducer.send(event);
    }


    @Transactional(readOnly = true)
    public List<RankingDto> getTodayRanking() {
        String key = createDailyRankingKey(LocalDate.now());

        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 9);

        if (tuples == null) {
            return List.of();
        }

        return tuples.stream()
                .map(tuple -> {
                    String keyword = tuple.getValue();
                    double score = tuple.getScore() == null ? 0 : tuple.getScore();

                    long price = menuRepository.findByName(keyword)
                            .map(menu -> menu.getPrice())
                            .orElse(0L);

                    return new RankingDto(keyword, score, price);
                })
                .toList();
    }

    private String createDailyRankingKey(LocalDate date) {
        return "popular:ranking:" + date;
    }
}