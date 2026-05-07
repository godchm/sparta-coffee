package org.sparta_coffee.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta_coffee.domain.menu.entity.Menu;
import org.sparta_coffee.global.entity.BaseEntity;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Getter
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문한 사용자 식별값
    @Column(nullable = false)
    private Long userId;


    @Column(nullable = false)
    private long paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime orderedAt;



    @Builder
    public Order(Long userId, long paymentAmount, OrderStatus status, LocalDateTime orderedAt) {
        this.userId = userId;
        this.paymentAmount = paymentAmount;
        this.status = status;
        this.orderedAt = orderedAt;
    }


    public void updatePaymentAmount(long paymentAmount) {
        this.paymentAmount = paymentAmount;
    }


    // 주문 취소 상태로 변경.
    public void cancel() {
        this.status = OrderStatus.CANCELED;
    }

    // 결제 상태로 변경
    public void pay() {
        this.status = OrderStatus.PAID;
    }
}
