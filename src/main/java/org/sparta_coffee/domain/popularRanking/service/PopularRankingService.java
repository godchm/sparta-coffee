package org.sparta_coffee.domain.popularRanking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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

    // 인기 검색어 등록
    // 사용자가 검색어를 입력하면 오늘 날짜 기준 Redis ZSET의 score를 1 증가시킨다.
    @Transactional
    public void search(PopularRankingRequest request, Long userId) {
        String key = createSearchRankingKey(LocalDate.now());

        stringRedisTemplate.opsForZSet()
                .incrementScore(key, request.keyword(), 1);
    }

    // 인기 검색어 조회
    // 오늘 날짜 기준 인기 검색어 TOP 10을 조회한다.
    @Transactional(readOnly = true)
    public List<RankingDto> getTodayRanking() {
        String key = createSearchRankingKey(LocalDate.now());

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

                    String rankingDateTime = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm EEEE", Locale.KOREAN));

                    return new RankingDto(score, keyword, price, rankingDateTime);
                })
                .toList();
    }

    // 최근 7일 인기 메뉴 TOP 3 조회
    @Transactional(readOnly = true)
    public List<RankingDto> getPopularMenus() {
        String tempKey = "popular:menu:temp:" + LocalDateTime.now();

        List<String> keys = java.util.stream.IntStream.range(0, 7)
                .mapToObj(i -> createMenuRankingKey(LocalDate.now().minusDays(i)))
                .toList();

        stringRedisTemplate.opsForZSet()
                .unionAndStore(keys.get(0), keys.subList(1, keys.size()), tempKey);

        stringRedisTemplate.expire(tempKey, java.time.Duration.ofMinutes(1));

        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(tempKey, 0, 2);

        if (tuples == null) {
            return List.of();
        }

        return tuples.stream()
                .map(tuple -> {
                    Long menuId = Long.valueOf(tuple.getValue());
                    double score = tuple.getScore() == null ? 0 : tuple.getScore();

                    String rankingDateTime = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm EEEE", Locale.KOREAN));

                    return menuRepository.findByIdAndActiveTrue(menuId)
                            .map(menu -> new RankingDto(score, menu.getName(), menu.getPrice(), rankingDateTime))
                            .orElse(null);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }


    // 검색어 랭킹 Redis key
    private String createSearchRankingKey(LocalDate date) {
        return "popular:search:" + date;
    }

    // 주문 기반 인기 메뉴 Redis key
    private String createMenuRankingKey(LocalDate date) {
        return "popular:menu:" + date;
    }
}
