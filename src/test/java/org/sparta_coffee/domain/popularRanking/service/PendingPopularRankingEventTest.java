package org.sparta_coffee.domain.popularRanking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta_coffee.common.config.model.kafka.event.PopularRankingEvent;
import org.sparta_coffee.domain.popularRanking.entity.PendingPopularRankingEvent;
import org.sparta_coffee.domain.popularRanking.entity.PopularRankingEventStatus;
import org.sparta_coffee.domain.popularRanking.producer.PopularRankingProducer;
import org.sparta_coffee.domain.popularRanking.repository.PendingPopularRankingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;


/*
 * PendingPopularRankingEvent 테스트 시나리오
 *
 * 이 테스트는 결제 성공 이후 인기 메뉴 집계 이벤트를 Kafka로 바로 전송하지 않고,
 * pending_popular_ranking_events 테이블에 먼저 저장한 뒤 비동기로 전송하는 구조를 검증한다.
 *
 * 이 구조를 사용하는 이유:
 * - 결제는 핵심 기능이고, Kafka 이벤트 전송은 후속 처리이다.
 * - Kafka 장애가 발생해도 주문 결제 데이터는 DB에 정상 저장되어야 한다.
 * - 이벤트 전송에 실패한 경우에도 DB에 남겨두고 나중에 재시도할 수 있어야 한다.
 * - 일정 횟수 이상 실패한 이벤트는 FAILED 상태로 분리하고, 서버 재시작 시 다시 복구할 수 있어야 한다.
 *
 * 상태 흐름:
 * PENDING
 *   -> PROCESSING
 *   -> SENT
 *
 * 실패 시:
 * PENDING
 *   -> PROCESSING
 *   -> PENDING, retryCount 증가
 *
 * 5회 실패 시:
 * PENDING
 *   -> PROCESSING
 *   -> FAILED
 *
 * 서버 재시작 복구 시:
 * FAILED 또는 PROCESSING
 *   -> PENDING
 *   -> Kafka 재전송 성공 시 SENT
 */

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class PendingPopularRankingEventTest {

    @Autowired
    private PendingPopularRankingEventPublisher publisher;

    @Autowired
    private PendingPopularRankingEventRepository pendingPopularRankingEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PopularRankingProducer popularRankingProducer;

    @Test
    @DisplayName("Kafka 전송 실패 시 이벤트는 재시도 대상 상태로 남고 retryCount가 증가한다")
    void publishPendingEventFail() throws Exception {

        /*
         * 시나리오:
         * 1. PENDING 상태의 인기 메뉴 이벤트를 DB에 저장한다.
         * 2. Kafka 전송이 실패하도록 Mock 처리한다.
         * 3. 이벤트 발행 로직을 실행한다.
         * 4. 이벤트가 삭제되거나 실패 처리되는 것이 아니라 PENDING 상태로 남는지 확인한다.
         * 5. retryCount가 1 증가했는지 확인한다.
         *
         * 이유:
         * 일시적인 Kafka 장애라면 이벤트를 바로 버리면 안 된다.
         * PENDING 상태로 남겨야 다음 스케줄러 실행 때 다시 전송할 수 있다.
         */

        pendingPopularRankingEventRepository.deleteAll();

        PendingPopularRankingEvent pendingEvent = createPendingEvent();

        doThrow(new RuntimeException("Kafka 장애"))
                .when(popularRankingProducer)
                .sendSync(any(PopularRankingEvent.class));

        publisher.publishPendingEvents();

        PendingPopularRankingEvent result = pendingPopularRankingEventRepository.findById(pendingEvent.getId())
                .orElseThrow();

        assertThat(result.getStatus()).isEqualTo(PopularRankingEventStatus.PENDING);
        assertThat(result.getRetryCount()).isEqualTo(1);
        assertThat(result.getSentAt()).isNull();
        assertThat(result.getLastErrorMessage()).contains("Kafka 장애");
    }

    @Test
    @DisplayName("Kafka 전송이 5회 실패하면 이벤트는 FAILED 상태가 된다")
    void publishPendingEventFailOverMaxRetryCount() throws Exception {

        /*
         * 시나리오:
         * 1. PENDING 이벤트를 저장한다.
         * 2. Kafka 전송이 계속 실패하도록 만든다.
         * 3. 전송 재시도를 5번 수행한다.
         * 4. retryCount가 5가 되면 이벤트 상태가 FAILED로 변경되는지 확인한다.
         *
         * 이유:
         * 계속 실패하는 이벤트를 무한히 재시도하면 시스템 자원을 낭비할 수 있다.
         * 일정 횟수 이상 실패한 이벤트는 FAILED로 분리해서 장애 이벤트로 관리한다.
         */
        pendingPopularRankingEventRepository.deleteAll();

        PendingPopularRankingEvent pendingEvent = createPendingEvent();

        doThrow(new RuntimeException("Kafka 장애"))
                .when(popularRankingProducer)
                .sendSync(any(PopularRankingEvent.class));

        failPublishingFiveTimes(pendingEvent.getId());

        PendingPopularRankingEvent result = pendingPopularRankingEventRepository.findById(pendingEvent.getId())
                .orElseThrow();

        assertThat(result.getStatus()).isEqualTo(PopularRankingEventStatus.FAILED);
        assertThat(result.getRetryCount()).isEqualTo(5);
        assertThat(result.getSentAt()).isNull();
    }

    @Test
    @DisplayName("FAILED 이벤트는 서버 재시작 복구 로직으로 PENDING 상태가 된다")
    void recoverFailedEvent() throws Exception {

        /*
         * 시나리오:
         * 1. Kafka 전송을 5번 실패시켜 이벤트를 FAILED 상태로 만든다.
         * 2. 서버 재시작 시 실행되는 복구 로직을 직접 호출한다.
         * 3. FAILED 이벤트가 다시 PENDING 상태로 변경되는지 확인한다.
         * 4. retryCount가 0으로 초기화되는지 확인한다.
         *
         * 이유:
         * Kafka 장애가 오래 지속되면 이벤트가 FAILED 상태로 남을 수 있다.
         * 이후 서버를 다시 시작했을 때 FAILED 이벤트를 다시 전송 대상으로 복구하면,
         * 운영자가 수동으로 DB를 수정하지 않아도 이벤트 재처리가 가능하다.
         */

        pendingPopularRankingEventRepository.deleteAll();

        PendingPopularRankingEvent pendingEvent = createPendingEvent();

        doThrow(new RuntimeException("Kafka 장애"))
                .when(popularRankingProducer)
                .sendSync(any(PopularRankingEvent.class));

        failPublishingFiveTimes(pendingEvent.getId());

        PendingPopularRankingEvent failedEvent = pendingPopularRankingEventRepository.findById(pendingEvent.getId())
                .orElseThrow();

        assertThat(failedEvent.getStatus()).isEqualTo(PopularRankingEventStatus.FAILED);

        publisher.recoverFailedAndStuckEvents();

        PendingPopularRankingEvent result = pendingPopularRankingEventRepository.findById(pendingEvent.getId())
                .orElseThrow();

        assertThat(result.getStatus()).isEqualTo(PopularRankingEventStatus.PENDING);
        assertThat(result.getRetryCount()).isEqualTo(0);
        assertThat(result.getNextRetryAt()).isNotNull();
    }

    @Test
    @DisplayName("5회 실패로 FAILED 된 이벤트도 서버 재시작 복구 후 Kafka 전송에 성공하면 SENT가 된다")
    void failedEventCanBePublishedAfterApplicationRestartRecovery() throws Exception {

        /*
         * 시나리오:
         * 1. Kafka 장애 상황을 만들어 이벤트를 5번 실패시킨다.
         * 2. 이벤트가 FAILED 상태가 되었는지 확인한다.
         * 3. 서버 재시작 복구 로직을 실행해 FAILED 이벤트를 PENDING으로 되돌린다.
         * 4. Kafka가 복구된 상황으로 Mock 설정을 변경한다.
         * 5. 이벤트 발행 로직을 다시 실행한다.
         * 6. 최종적으로 이벤트가 SENT 상태가 되는지 확인한다.
         *
         * 이유:
         * 이 테스트는 outbox 방식의 핵심 목적을 검증한다.
         * 결제 시점에 Kafka 전송이 실패해도 이벤트를 DB에 보존하고,
         * Kafka가 복구된 뒤 정상적으로 재전송할 수 있어야 한다.
         */

        pendingPopularRankingEventRepository.deleteAll();

        PendingPopularRankingEvent pendingEvent = createPendingEvent();

        doThrow(new RuntimeException("Kafka 장애"))
                .when(popularRankingProducer)
                .sendSync(any(PopularRankingEvent.class));

        failPublishingFiveTimes(pendingEvent.getId());

        PendingPopularRankingEvent failedEvent = pendingPopularRankingEventRepository.findById(pendingEvent.getId())
                .orElseThrow();

        assertThat(failedEvent.getStatus()).isEqualTo(PopularRankingEventStatus.FAILED);
        assertThat(failedEvent.getRetryCount()).isEqualTo(5);

        publisher.recoverFailedAndStuckEvents();

        reset(popularRankingProducer);

        doNothing()
                .when(popularRankingProducer)
                .sendSync(any(PopularRankingEvent.class));

        publisher.publishPendingEvents();

        PendingPopularRankingEvent result = pendingPopularRankingEventRepository.findById(pendingEvent.getId())
                .orElseThrow();

        assertThat(result.getStatus()).isEqualTo(PopularRankingEventStatus.SENT);
        assertThat(result.getRetryCount()).isEqualTo(0);
        assertThat(result.getSentAt()).isNotNull();
    }

    private PendingPopularRankingEvent createPendingEvent() throws Exception {
        PopularRankingEvent event = new PopularRankingEvent(
                1L,
                "아메리카노",
                3000L,
                1L,
                3000L,
                LocalDateTime.now(),
                1
        );

        PendingPopularRankingEvent pendingEvent = PendingPopularRankingEvent.builder()
                .payload(objectMapper.writeValueAsString(event))
                .build();

        return pendingPopularRankingEventRepository.saveAndFlush(pendingEvent);
    }

    private void failPublishingFiveTimes(Long eventId) {
        /*
         * 테스트에서는 실제 스케줄러처럼 10초, 30초를 기다릴 수 없다.
         * markPublishFailed()가 실패 후 nextRetryAt을 미래 시간으로 미루기 때문에,
         * 다음 publishPendingEvents() 호출에서 바로 조회되지 않는다.
         *
         * 그래서 테스트에서만 ReflectionTestUtils로 nextRetryAt을 과거 시간으로 바꿔
         * 즉시 재시도 가능한 이벤트처럼 만든다.
         *
         * 주의:
         * recover()를 여기서 사용하면 안 된다.
         * recover()는 서버 재시작 복구용 메서드라 retryCount를 0으로 초기화한다.
         * retryCount가 초기화되면 5회 실패 후 FAILED가 되는 흐름을 테스트할 수 없다.
         */


        for (int i = 0; i < 5; i++) {
            publisher.publishPendingEvents();

            PendingPopularRankingEvent savedEvent = pendingPopularRankingEventRepository.findById(eventId)
                    .orElseThrow();

            if (i < 4) {
                ReflectionTestUtils.setField(
                        savedEvent,
                        "nextRetryAt",
                        LocalDateTime.now().minusSeconds(1)
                );

                pendingPopularRankingEventRepository.saveAndFlush(savedEvent);
            }
        }
    }
}