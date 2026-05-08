package org.sparta_coffee.domain.popularRanking.runner;

import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.popularRanking.service.PendingPopularRankingEventPublisher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingPopularRankingEventStartupRunner {

    private final PendingPopularRankingEventPublisher publisher;

    @EventListener(ApplicationReadyEvent.class)
    public void publishOnStartup() {
        publisher.recoverFailedAndStuckEvents();
        publisher.publishPendingEvents();
    }
}