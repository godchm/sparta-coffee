package org.sparta_coffee.domain.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta_coffee.global.entity.BaseEntity;



@Entity
@Getter
@Table(
        name = "user_points",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_points_user_id", columnNames = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 식별값
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private long balance;


    // 낙관적 락 버전 필드
    // 같은 포인트 row를 동시에 수정하면 version 충돌로 감지됨
    @Version
    private Long version;


    @Builder
    public UserPoint(Long userId, long balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public void charge(long amount) {

        this.balance += amount;
    }

    public void use(long amount) {
        this.balance -= amount;
    }

    public void refund(long amount) {
        this.balance += amount;
    }
}
