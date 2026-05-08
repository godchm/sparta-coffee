package org.sparta_coffee.domain.point.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta_coffee.domain.point.entity.UserPoint;
import org.sparta_coffee.domain.point.repository.UserPointRepository;
import org.sparta_coffee.domain.user.entity.User;
import org.sparta_coffee.domain.user.enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sparta_coffee.domain.user.repository.UserRepository;

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
    private UserRepository userRepository;

    @Test
    @DisplayName("동시에 같은 사용자 포인트를 차감하면 낙관락으로 충돌을 감지하고 재시도한다")
    void usePointWithOptimisticLock() throws InterruptedException {

        // 사용자의 최초 포인트.
        // 10000P에서 여러 스레드가 동시에 1000P씩 차감한다.
        long initialBalance = 10000L;

        // 한 번 결제할 때 차감할 포인트.
        long useAmount = 1000L;

        // 동시에 실행할 요청 개수.
        // 즉, 같은 사용자 포인트를 10개 요청이 동시에 차감하려고 시도한다.
        int threadCount = 10;



        // given
        userPointRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(
                User.builder()
                        .name("테스트유저")
                        .email("point-test@example.com")
                        .password("password")
                        .role(UserRole.USER)
                        .build()
        );

        Long userId = user.getId();

        UserPoint userPoint = UserPoint.builder()
                .user(user)
                .balance(initialBalance)
                .build();

        userPointRepository.save(userPoint);

        // 10개의 스레드를 가진 스레드 풀을 만든다.
        // 실제 동시에 여러 요청이 들어오는 상황을 흉내 내기 위함이다.
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // 모든 스레드가 준비될 때까지 기다리는 latch.
        CountDownLatch readyLatch = new CountDownLatch(threadCount);

        // 모든 스레드를 거의 동시에 출발시키기 위한 latch.
        CountDownLatch startLatch = new CountDownLatch(1);

        // 모든 스레드가 작업을 끝낼 때까지 기다리는 latch.
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // 성공한 차감 요청 수.
        AtomicInteger successCount = new AtomicInteger();

        // 실패한 차감 요청 수.
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 현재 스레드가 준비되었음을 알린다.
                    readyLatch.countDown();

                    // 모든 스레드가 준비될 때까지 대기한다.
                    // startLatch.countDown()이 호출되면 동시에 실행된다.
                    startLatch.await();

                    // 같은 userId의 포인트를 1000P 차감한다.
                    // 여러 스레드가 동시에 같은 UserPoint row를 수정하므로 version 충돌이 발생할 수 있다.
                    pointService.use(userId, useAmount);

                    // 예외 없이 차감에 성공하면 성공 카운트 증가.
                    successCount.incrementAndGet();
                } catch (Exception exception) {
                    // 낙관락 재시도 후에도 실패하거나 잔액 부족 등이 발생하면 실패 카운트 증가.
                    failCount.incrementAndGet();
                } finally {
                    // 현재 스레드 작업 완료.
                    doneLatch.countDown();
                }
            });
        }

        // 모든 스레드가 준비될 때까지 기다린다.
        readyLatch.await();

        // 준비된 스레드들을 거의 동시에 출발시킨다.
        startLatch.countDown();

        // 모든 스레드가 끝날 때까지 기다린다.
        doneLatch.await();

        executorService.shutdown();

        // then
        // 최종 포인트 상태를 DB에서 다시 조회한다.
        UserPoint resultPoint = userPointRepository.findByUser_Id(userId)
                .orElseThrow();

        // 최종 잔액은 "최초 포인트 - 성공한 차감 횟수 * 차감 금액"이어야 한다.
        // 즉, 실패한 요청은 포인트를 차감하면 안 된다.
        long expectedBalance = initialBalance - (successCount.get() * useAmount);

        assertThat(resultPoint.getBalance()).isEqualTo(expectedBalance);

        // 성공 횟수 + 실패 횟수는 전체 요청 개수와 같아야 한다.
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);

        // 성공한 요청이 있다면 version 값도 증가해야 한다.
        // UserPoint의 @Version이 정상 동작하는지 간접적으로 확인한다.
        assertThat(resultPoint.getVersion()).isGreaterThanOrEqualTo(successCount.get());
    }
}
