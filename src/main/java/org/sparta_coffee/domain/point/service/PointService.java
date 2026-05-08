package org.sparta_coffee.domain.point.service;

import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.point.dto.request.PointChargeRequest;
import org.sparta_coffee.domain.point.dto.response.PointChargeResponse;
import org.sparta_coffee.domain.point.dto.response.PointResponse;
import org.sparta_coffee.domain.point.entity.PointHistory;
import org.sparta_coffee.domain.point.entity.PointHistoryType;
import org.sparta_coffee.domain.point.entity.UserPoint;
import org.sparta_coffee.domain.point.repository.PointHistoryRepository;
import org.sparta_coffee.domain.point.repository.UserPointRepository;
import org.sparta_coffee.domain.user.entity.User;
import org.sparta_coffee.domain.user.enums.UserRole;
import org.sparta_coffee.domain.user.repository.UserRepository;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.PointException;
import org.sparta_coffee.global.exception.domain.UserException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;


    @Transactional
    public PointChargeResponse charge(PointChargeRequest request, Long loginUserId) {
        if (request.amount() <= 0) {
            throw new PointException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }

        User user = findUser(loginUserId);

        UserPoint userPoint = userPointRepository.findByUser_Id(loginUserId)
                .orElseGet(() -> UserPoint.builder()
                        .user(findUser(loginUserId))
                        .balance(0)
                        .build());


        userPoint.charge(request.amount());

        UserPoint savedUserPoint = userPointRepository.save(userPoint);


        PointHistory pointHistory = PointHistory.builder()
                .user(user)
                .amount(request.amount())
                .type(PointHistoryType.CHARGE)
                .balanceAfter(savedUserPoint.getBalance())
                .build();

        pointHistoryRepository.save(pointHistory);

        return PointChargeResponse.builder()
                .userId(savedUserPoint.getUser().getId())
                .chargedAmount(request.amount())
                .balance(savedUserPoint.getBalance())
                .build();
    }

    @Transactional(readOnly = true)
    public PointResponse getPoint(Long targetUserId, Long loginUserId, UserRole loginUserRole) {
        validateOwnerOrAdmin(targetUserId, loginUserId, loginUserRole);

        UserPoint userPoint = userPointRepository.findByUser_Id(targetUserId)
                .orElseThrow(() -> new PointException(ErrorCode.POINT_NOT_FOUND));

        return PointResponse.from(userPoint);
    }

    @Transactional
    public void deletePoint(Long targetUserId, Long loginUserId, UserRole loginUserRole) {
        validateOwnerOrAdmin(targetUserId, loginUserId, loginUserRole);

        UserPoint userPoint = userPointRepository.findByUser_Id(targetUserId)
                .orElseThrow(() -> new PointException(ErrorCode.POINT_NOT_FOUND));

        userPointRepository.delete(userPoint);
    }

    private void validateOwnerOrAdmin(Long targetUserId, Long loginUserId, UserRole loginUserRole) {
        if (loginUserRole == UserRole.ADMIN) {
            return;
        }

        if (!targetUserId.equals(loginUserId)) {
            throw new PointException(ErrorCode.ACCESS_DENIED);
        }
    }

    // 낙관적 락 어노테이션으로 구현.
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    @Transactional
    public long use(Long userId, long amount) {
        if (amount <= 0) {
            throw new PointException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }

        UserPoint userPoint = userPointRepository.findByUser_Id(userId)
                .orElseThrow(() -> new PointException(ErrorCode.INSUFFICIENT_POINT));

        if (userPoint.getBalance() < amount) {
            throw new PointException(ErrorCode.INSUFFICIENT_POINT);
        }

        userPoint.use(amount);

        User user = findUser(userId);

        PointHistory pointHistory = PointHistory.builder()
                .user(user)
                .amount(amount)
                .type(PointHistoryType.USE)
                .balanceAfter(userPoint.getBalance())
                .build();

        pointHistoryRepository.save(pointHistory);

        return userPoint.getBalance();
    }
    // 낙관적 락 에러 응답 코드.
    @Recover
    public long recoverUse(
            OptimisticLockingFailureException exception,
            Long userId,
            long amount
    ) {
        throw new PointException(ErrorCode.CONFLICT_POINT_UPDATE);
    }



 // 환불 메서드
    @Transactional
    public long refund(Long userId, long amount) {
        if (amount <= 0) {
            throw new PointException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }

        UserPoint userPoint = userPointRepository.findByUser_Id(userId)
                .orElseThrow(() -> new PointException(ErrorCode.POINT_NOT_FOUND));

        userPoint.refund(amount);

        User user = findUser(userId);

        PointHistory pointHistory = PointHistory.builder()
                .user(user)
                .amount(amount)
                .type(PointHistoryType.REFUND)
                .balanceAfter(userPoint.getBalance())
                .build();

        pointHistoryRepository.save(pointHistory);

        return userPoint.getBalance();
    }

// 유저 찾기
private User findUser(Long userId) {
    return userRepository.findById(userId)
            .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
}
}
