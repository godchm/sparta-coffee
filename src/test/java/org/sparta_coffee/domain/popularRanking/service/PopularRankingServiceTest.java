package org.sparta_coffee.domain.popularRanking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta_coffee.common.config.model.redis.RankingDto;
import org.sparta_coffee.domain.menu.entity.Menu;
import org.sparta_coffee.domain.menu.repository.MenuRepository;
import org.sparta_coffee.domain.popularRanking.producer.PopularRankingProducer;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularRankingServiceTest {

    @Mock
    private PopularRankingProducer popularRankingProducer;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private PopularRankingService popularRankingService;

    @Test
    @DisplayName("최근 7일 인기 메뉴 TOP 3를 조회한다")
    void getPopularMenus() {
        /*
         * given
         *
         * 최근 7일간의 Redis ZSET을 합산한 결과가
         * 판매 수량 기준 TOP 3로 반환된 상황을 가정한다.
         */
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(new DefaultTypedTuple<>("1", 12.0));
        tuples.add(new DefaultTypedTuple<>("2", 8.0));
        tuples.add(new DefaultTypedTuple<>("3", 5.0));

        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores(anyString(), eq(0L), eq(2L)))
                .thenReturn(tuples);

        when(menuRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(Menu.builder()
                        .name("아메리카노")
                        .price(3000)
                        .build()));

        when(menuRepository.findByIdAndActiveTrue(2L))
                .thenReturn(Optional.of(Menu.builder()
                        .name("카페라떼")
                        .price(3500)
                        .build()));

        when(menuRepository.findByIdAndActiveTrue(3L))
                .thenReturn(Optional.of(Menu.builder()
                        .name("바닐라라떼")
                        .price(4000)
                        .build()));

        /*
         * when
         */
        List<RankingDto> result = popularRankingService.getPopularMenus();

        /*
         * then
         */
        assertThat(result).hasSize(3);

        assertThat(result.get(0).getScore()).isEqualTo(12.0);
        assertThat(result.get(0).getKeyword()).isEqualTo("아메리카노");
        assertThat(result.get(0).getPrice()).isEqualTo(3000);

        assertThat(result.get(1).getScore()).isEqualTo(8.0);
        assertThat(result.get(1).getKeyword()).isEqualTo("카페라떼");
        assertThat(result.get(1).getPrice()).isEqualTo(3500);

        assertThat(result.get(2).getScore()).isEqualTo(5.0);
        assertThat(result.get(2).getKeyword()).isEqualTo("바닐라라떼");
        assertThat(result.get(2).getPrice()).isEqualTo(4000);

        /*
         * 최근 7일치 Redis key를 합산해 임시 key에 저장하고,
         * 임시 key는 짧은 시간 뒤 만료되도록 설정해야 한다.
         */
        String todayKey = "popular:menu:" + LocalDate.now();
        List<String> otherKeys = java.util.stream.IntStream.range(1, 7)
                .mapToObj(i -> "popular:menu:" + LocalDate.now().minusDays(i))
                .toList();

        ArgumentCaptor<String> tempKeyCaptor = ArgumentCaptor.forClass(String.class);

        verify(zSetOperations).unionAndStore(
                eq(todayKey),
                eq(otherKeys),
                tempKeyCaptor.capture()
        );

        assertThat(tempKeyCaptor.getValue()).startsWith("popular:menu:temp:");

        verify(stringRedisTemplate).expire(
                eq(tempKeyCaptor.getValue()),
                eq(Duration.ofMinutes(1))
        );

        verify(zSetOperations).reverseRangeWithScores(
                eq(tempKeyCaptor.getValue()),
                eq(0L),
                eq(2L)
        );
    }
}
