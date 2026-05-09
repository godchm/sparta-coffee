package org.sparta_coffee.domain.point.service;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta_coffee.domain.point.repository.PointHistoryRepository;
import org.sparta_coffee.domain.point.repository.UserPointRepository;
import org.sparta_coffee.domain.user.repository.UserRepository;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.PointException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class PointRetryExceededTest {

    @Autowired
    private PointService pointService;

    @MockitoBean
    private UserPointRepository userPointRepository;

    @MockitoBean
    private PointHistoryRepository pointHistoryRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("낙관락 충돌이 재시도 횟수를 초과하면 CONFLICT_POINT_UPDATE 예외가 발생한다")
    void optimisticLockRetryExceededThrowsConflictPointUpdate() {
        /*
         * given
         *
         * PointService.use()는 OptimisticLockingFailureException에 대해
         * @Retryable(maxAttempts = 3)이 적용되어 있다.
         *
         * userPointRepository가 계속 낙관락 예외를 던지도록 만들어
         * 재시도 횟수를 초과하는 상황을 강제로 만든다.
         */
        Long userId = 1L;
        long useAmount = 1000L;

        doThrow(new OptimisticLockingFailureException("낙관락 충돌"))
                .when(userPointRepository)
                .findByUser_Id(userId);

        /*
         * when & then
         *
         * 낙관락 예외가 3번 모두 발생하면
         * @Recover 메서드가 실행되어 CONFLICT_POINT_UPDATE가 발생해야 한다.
         */
        assertThatThrownBy(() -> pointService.use(userId, useAmount))
                .isInstanceOf(PointException.class)
                .satisfies(exception -> {
                    PointException pointException = (PointException) exception;
                    assertThat(pointException.getErrorCode())
                            .isEqualTo(ErrorCode.CONFLICT_POINT_UPDATE);
                });

        /*
         * maxAttempts = 3 이므로 총 3번 호출되어야 한다.
         */
        verify(userPointRepository, times(3)).findByUser_Id(userId);

        /*
         * 최종 실패했으므로 히스토리는 저장되면 안 된다.
         */
        verify(userRepository, never()).findById(any());
        verify(pointHistoryRepository, never()).save(any());
    }
}