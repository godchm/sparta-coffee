package org.sparta_coffee.domain.point.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta_coffee.domain.point.entity.UserPoint;
import org.sparta_coffee.domain.point.repository.PointHistoryRepository;
import org.sparta_coffee.domain.point.repository.UserPointRepository;
import org.sparta_coffee.domain.user.entity.User;
import org.sparta_coffee.domain.user.enums.UserRole;
import org.sparta_coffee.domain.user.repository.UserRepository;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.PointException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.AopTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PointInsufficientRetryPolicyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("잔액 부족 예외는 낙관락 재시도 대상이 아니다")
    void insufficientPointIsNotRetryTarget() throws Exception {
        /*
         * given
         *
         * 실제 DB에 500P를 가진 사용자를 저장한다.
         * 이후 1000P 차감을 요청해 잔액 부족 상황을 만든다.
         */
        pointHistoryRepository.deleteAll();
        userPointRepository.deleteAll();

        User user = userRepository.save(
                User.builder()
                        .name("포인트 부족 테스트 유저")
                        .email("insufficient-point-" + System.nanoTime() + "@example.com")
                        .password("password")
                        .role(UserRole.USER)
                        .build()
        );

        Long userId = user.getId();

        userPointRepository.save(
                UserPoint.builder()
                        .user(user)
                        .balance(500L)
                        .build()
        );

        /*
         * PointService는 @Retryable 때문에 프록시 객체로 주입된다.
         * 여기서는 retry 동작이 아니라,
         * 잔액 부족이라는 비즈니스 예외가 use() 내부에서 즉시 발생하는지를 검증한다.
         */
        PointService targetPointService = AopTestUtils.getTargetObject(pointService);

        /*
         * when & then
         *
         * 잔액 부족은 낙관락 충돌이 아니라 비즈니스 예외다.
         * 따라서 PointException(INSUFFICIENT_POINT)이 발생해야 한다.
         */
        assertThatThrownBy(() -> targetPointService.use(userId, 1000L))
                .isInstanceOf(PointException.class)
                .satisfies(exception -> {
                    PointException pointException = (PointException) exception;
                    assertThat(pointException.getErrorCode())
                            .isEqualTo(ErrorCode.INSUFFICIENT_POINT);
                });

        /*
         * 차감 실패했으므로 잔액은 그대로 유지되어야 한다.
         */
        UserPoint resultPoint = userPointRepository.findByUser_Id(userId)
                .orElseThrow();

        assertThat(resultPoint.getBalance()).isEqualTo(500L);

        /*
         * 잔액 부족은 차감 전에 실패하므로 히스토리가 저장되면 안 된다.
         */
        assertThat(pointHistoryRepository.count()).isZero();

        /*
         * 차감이 일어나지 않았으므로 version도 증가하지 않는다.
         */
        assertThat(resultPoint.getVersion()).isZero();
    }
}