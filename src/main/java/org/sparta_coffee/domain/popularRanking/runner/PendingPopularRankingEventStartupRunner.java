package org.sparta_coffee.domain.popularRanking.runner;

import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.popularRanking.service.PendingPopularRankingEventPublisher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 미처리 인기 메뉴 이벤트를 복구하고 재발행하는 Runner이다.
 *
 * Kafka 장애나 애플리케이션 종료로 인해 이벤트가 FAILED 또는 PROCESSING 상태로 남을 수 있다.
 * 서버가 다시 시작되면 해당 이벤트를 재처리 가능한 상태로 복구한 뒤,
 * PENDING 이벤트를 Kafka로 다시 발행한다.
 *
 * 즉, 카프카 서버가 죽었을 경우룰 방어하기 위한 로직이다.
 */


@Component
@RequiredArgsConstructor
public class PendingPopularRankingEventStartupRunner {

    private final PendingPopularRankingEventPublisher publisher;

    /**
     * 애플리케이션이 완전히 준비된 이후 실행된다.
     *
     * 1. FAILED 또는 PROCESSING 상태로 남아 있는 이벤트를 PENDING 상태로 복구.
     * 2. 복구된 이벤트를 포함해 발행 가능한 PENDING 이벤트를 Kafka로 전송.
     *
     * 이를 통해 서버 재시작 이후에도 미전송 이벤트가 유실되지 않고 다시 처리가 가능하다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void publishOnStartup() {
        publisher.recoverFailedAndStuckEvents();
        publisher.publishPendingEvents();
    }
}