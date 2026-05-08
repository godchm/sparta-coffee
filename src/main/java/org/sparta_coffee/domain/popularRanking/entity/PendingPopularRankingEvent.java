package org.sparta_coffee.domain.popularRanking.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta_coffee.global.entity.BaseEntity;

import java.time.LocalDateTime;

// 카프카 전송에 실패했을 경우 데이터를 DB의 저장하기 위해서 엔티티 구현.

@Entity
@Getter
@Table(name = "pending_popular_ranking_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingPopularRankingEvent extends BaseEntity {

    private static final int MAX_RETRY_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false)
    private String payload;

    // 카프카 상태 변화 필드들
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PopularRankingEventStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime nextRetryAt;

    private LocalDateTime sentAt;

    @Column(length = 500)
    private String lastErrorMessage;

    @Builder
    public PendingPopularRankingEvent(String payload) {
        this.payload = payload;
        this.status = PopularRankingEventStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = LocalDateTime.now();
    }

    public void markProcessing() {
        this.status = PopularRankingEventStatus.PROCESSING;
    }

    public void markSent() {
        this.status = PopularRankingEventStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.lastErrorMessage = null;
    }

    public void markPublishFailed(String errorMessage) {
        this.retryCount++;
        this.lastErrorMessage = truncate(errorMessage);

        if (this.retryCount >= MAX_RETRY_COUNT) {
            this.status = PopularRankingEventStatus.FAILED;
            return;
        }

        this.status = PopularRankingEventStatus.PENDING;
        this.nextRetryAt = LocalDateTime.now().plusSeconds(calculateRetryDelaySeconds());
    }

    public void recover() {
        this.status = PopularRankingEventStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = LocalDateTime.now();
    }

    private long calculateRetryDelaySeconds() {
        return switch (retryCount) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 60;
            case 4 -> 300;
            default -> 600;
        };
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }

        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}