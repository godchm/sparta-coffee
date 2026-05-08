package org.sparta_coffee.domain.popularRanking.scheduler;


import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.popularRanking.service.PendingPopularRankingEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 재시도 로직들

@Component
@RequiredArgsConstructor
public class PendingPopularRankingEventScheduler {

    private final PendingPopularRankingEventPublisher publisher;

    @Scheduled(fixedDelay = 5000)
    public void publishPeriodically() {
        publisher.publishPendingEvents();
    }
}