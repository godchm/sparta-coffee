package org.sparta_coffee.domain.order.entity;

public enum OrderStatus {
    PENDING,   // 주문 생성됨, 결제 진행 전 또는 진행 중
    PAID,      // 결제 완료
    CANCELED,  // 주문 취소
    FAILED     // 결제 실패
}
