package org.sparta_coffee.domain.point.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta_coffee.domain.point.entity.UserPoint;
import org.sparta_coffee.domain.point.repository.PointHistoryRepository;
import org.sparta_coffee.domain.point.repository.UserPointRepository;
import org.sparta_coffee.domain.user.entity.User;
import org.sparta_coffee.domain.user.enums.UserRole;
import org.sparta_coffee.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointOptimisticLockTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("같은 사용자 포인트를 동시에 차감하면 낙관락으로 정합성을 보장한다")
    void usePointWithOptimisticLock() throws InterruptedException {
        /*
         * given
         *
         * 하나의 UserPoint row를 여러 스레드가 동시에 수정한다.
         * UserPoint에는 @Version 필드가 있으므로 동시에 같은 row를 수정하면
         * OptimisticLockingFailureException이 발생할 수 있다.
         *
         * PointService.use()는 해당 예외에 대해 @Retryable을 적용하고 있으므로
         * 충돌이 발생해도 일부 요청은 재시도 후 성공할 수 있다.
         */
        long initialBalance = 10000L;
        long useAmount = 1000L;
        int threadCount = 10;

        pointHistoryRepository.deleteAll();
        userPointRepository.deleteAll();

        User user = userRepository.save(
                User.builder()
                        .name("포인트 낙관락 테스트 유저")
                        .email("point-optimistic-lock-" + System.nanoTime() + "@example.com")
                        .password("password")
                        .role(UserRole.USER)
                        .build()
        );

        Long userId = user.getId();

        userPointRepository.save(
                UserPoint.builder()
                        .user(user)
                        .balance(initialBalance)
                        .build()
        );

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        /*
         * when
         *
         * 10개의 요청을 거의 동시에 출발시켜
         * 같은 사용자의 포인트를 1000P씩 차감한다.
         */
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    pointService.use(userId, useAmount);

                    successCount.incrementAndGet();
                } catch (Exception exception) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        executorService.shutdown();

        /*
         * then
         *
         * 실패한 요청은 DB에 반영되면 안 된다.
         * 따라서 최종 잔액은 성공한 요청 수만큼만 차감되어야 한다.
         */
        UserPoint resultPoint = userPointRepository.findByUser_Id(userId)
                .orElseThrow();

        long expectedBalance = initialBalance - (successCount.get() * useAmount);

        assertThat(resultPoint.getBalance()).isEqualTo(expectedBalance);
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);

        /*
         * 성공한 차감 요청마다 PointHistory가 하나씩 저장된다.
         * 낙관락 충돌로 실패한 트랜잭션의 히스토리는 롤백되어야 한다.
         */
        assertThat(pointHistoryRepository.count()).isEqualTo(successCount.get());

        /*
         * @Version 값은 성공적으로 반영된 수정 횟수만큼 증가한다.
         * 최초 저장 후 version이 0부터 시작한다면,
         * 성공한 use 횟수만큼 version이 증가해야 한다.
         */
        assertThat(resultPoint.getVersion()).isEqualTo(successCount.get());
    }

    @Test
    @DisplayName("동시 차감 요청 금액이 보유 포인트보다 많아도 잔액은 음수가 되지 않는다")
    void concurrentUsePointDoesNotMakeNegativeBalance() throws InterruptedException {
        /*
         * given
         *
         * 3000P를 가진 사용자에게 10개의 요청이 동시에 1000P씩 차감 요청한다.
         * 정상이라면 최대 3번까지만 성공할 수 있고,
         * 나머지는 잔액 부족 또는 낙관락 충돌 재시도 실패로 실패해야 한다.
         */
        long initialBalance = 3000L;
        long useAmount = 1000L;
        int threadCount = 10;

        pointHistoryRepository.deleteAll();
        userPointRepository.deleteAll();

        User user = userRepository.save(
                User.builder()
                        .name("포인트 잔액 테스트 유저")
                        .email("point-balance-lock-" + System.nanoTime() + "@example.com")
                        .password("password")
                        .role(UserRole.USER)
                        .build()
        );

        Long userId = user.getId();

        userPointRepository.save(
                UserPoint.builder()
                        .user(user)
                        .balance(initialBalance)
                        .build()
        );

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        /*
         * when
         */
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    pointService.use(userId, useAmount);

                    successCount.incrementAndGet();
                } catch (Exception exception) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        executorService.shutdown();

        /*
         * then
         *
         * 동시에 차감 요청이 몰려도 성공한 요청 수만큼만 포인트가 차감되어야 한다.
         * 또한 잔액은 절대 음수가 되면 안 된다.
         */
        UserPoint resultPoint = userPointRepository.findByUser_Id(userId)
                .orElseThrow();

        long expectedBalance = initialBalance - (successCount.get() * useAmount);

        assertThat(resultPoint.getBalance()).isEqualTo(expectedBalance);
        assertThat(resultPoint.getBalance()).isGreaterThanOrEqualTo(0);
        assertThat(successCount.get()).isLessThanOrEqualTo(3);
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(pointHistoryRepository.count()).isEqualTo(successCount.get());
        assertThat(resultPoint.getVersion()).isEqualTo(successCount.get());
    }
}
