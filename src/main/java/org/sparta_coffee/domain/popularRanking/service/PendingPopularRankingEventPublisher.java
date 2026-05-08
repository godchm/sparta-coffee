package org.sparta_coffee.domain.popularRanking.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta_coffee.common.config.model.kafka.event.PopularRankingEvent;
import org.sparta_coffee.domain.popularRanking.entity.PendingPopularRankingEvent;
import org.sparta_coffee.domain.popularRanking.entity.PopularRankingEventStatus;
import org.sparta_coffee.domain.popularRanking.producer.PopularRankingProducer;
import org.sparta_coffee.domain.popularRanking.repository.PendingPopularRankingEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class PendingPopularRankingEventPublisher {

    private final PendingPopularRankingEventRepository repository;
    private final PopularRankingProducer popularRankingProducer;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publishPendingEvents() {
        List<PendingPopularRankingEvent> events =
                repository.findTop100ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        PopularRankingEventStatus.PENDING,
                        LocalDateTime.now()
                );

        for (PendingPopularRankingEvent event : events) {
            event.markProcessing();

            try {
                PopularRankingEvent payload = objectMapper.readValue(
                        event.getPayload(),
                        PopularRankingEvent.class
                );

                popularRankingProducer.sendSync(payload);
                event.markSent();
            } catch (Exception e) {
                log.warn("인기 메뉴 이벤트 발행 실패. eventId={}, retryCount={}", event.getId(), event.getRetryCount(), e);
                event.markPublishFailed(e.getMessage());
            }
        }
    }

    @Transactional
    public void recoverFailedAndStuckEvents() {
        List<PendingPopularRankingEvent> failedEvents =
                repository.findAllByStatus(PopularRankingEventStatus.FAILED);

        failedEvents.forEach(PendingPopularRankingEvent::recover);

        List<PendingPopularRankingEvent> processingEvents =
                repository.findAllByStatus(PopularRankingEventStatus.PROCESSING);

        processingEvents.forEach(PendingPopularRankingEvent::recover);
    }
}
