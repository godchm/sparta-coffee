package org.sparta_coffee.domain.popularRanking.repository;

import org.sparta_coffee.domain.popularRanking.entity.PendingPopularRankingEvent;
import org.sparta_coffee.domain.popularRanking.entity.PopularRankingEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;


public interface PendingPopularRankingEventRepository extends JpaRepository<PendingPopularRankingEvent, Long> {

    List<PendingPopularRankingEvent> findTop100ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            PopularRankingEventStatus status,
            LocalDateTime now
    );

    List<PendingPopularRankingEvent> findAllByStatus(PopularRankingEventStatus status);
}