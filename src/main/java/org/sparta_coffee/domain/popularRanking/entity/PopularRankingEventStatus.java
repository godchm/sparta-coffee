package org.sparta_coffee.domain.popularRanking.entity;


public enum PopularRankingEventStatus {
    PENDING, // 일단 보류
    PROCESSING, // 진행
    SENT,   // 성공할시 상태 변화. 카프카로 감.
    FAILED  // 카프카의 재시도 로직으로 했는데도 실패할경우
}