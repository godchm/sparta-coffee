package org.sparta_coffee.domain.order.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta_coffee.common.config.annotation.RedisLock;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.OrderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class OrderDistributedLockTest {

    @Autowired
    private RedisLockTestService redisLockTestService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("같은 key로 동시에 요청하면 Redis 분산락으로 하나의 요청만 실행된다")
    void sameKeyRequestOnlyOneSuccessByRedisLock() throws InterruptedException {
        /*
         * given
         *
         * 같은 lockTargetId로 10개의 요청을 동시에 보낸다.
         * RedisLockAspect는 첫 번째 파라미터를 기준으로 lock key를 만든다.
         *
         * lock key:
         * lock:test-order:1
         */
        Long lockTargetId = 1L;
        String lockKey = "lock:test-order:" + lockTargetId;

        int threadCount = 10;

        stringRedisTemplate.delete(lockKey);
        redisLockTestService.reset();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger lockFailCount = new AtomicInteger();
        AtomicInteger otherFailCount = new AtomicInteger();

        List<Exception> exceptions = new ArrayList<>();

        /*
         * when
         *
         * 10개의 스레드를 동시에 출발시킨다.
         * 테스트용 메서드는 락을 잡은 뒤 500ms 동안 대기한다.
         * 따라서 첫 번째 요청이 락을 점유하는 동안 나머지 요청은 같은 key의 락 획득에 실패해야 한다.
         */
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    redisLockTestService.executeWithLock(lockTargetId);

                    successCount.incrementAndGet();
                } catch (OrderException exception) {
                    exceptions.add(exception);

                    if (exception.getErrorCode() == ErrorCode.LOCK_ACQUIRE_FAILED) {
                        lockFailCount.incrementAndGet();
                    } else {
                        otherFailCount.incrementAndGet();
                    }
                } catch (Exception exception) {
                    exceptions.add(exception);
                    otherFailCount.incrementAndGet();
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
         * 같은 key에 대해 동시에 접근했으므로 실제 비즈니스 로직은 1번만 실행되어야 한다.
         * 나머지 9개 요청은 Redis 분산락 획득 실패로 종료되어야 한다.
         */
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(lockFailCount.get()).isEqualTo(threadCount - 1);
        assertThat(otherFailCount.get()).isZero();

        /*
         * 테스트용 서비스 내부 카운트도 1이어야 한다.
         * 즉, 락을 획득한 요청만 실제 메서드 내부로 진입했다는 뜻이다.
         */
        assertThat(redisLockTestService.getExecutedCount()).isEqualTo(1);

        /*
         * 메서드 실행이 끝난 뒤에는 Redis lock key가 반드시 삭제되어야 한다.
         * 락이 남아 있으면 이후 같은 주문 결제가 계속 막힐 수 있다.
         */
        assertThat(stringRedisTemplate.hasKey(lockKey)).isFalse();
    }

    @TestConfiguration
    static class RedisLockTestConfig {

        @Bean
        public RedisLockTestService redisLockTestService() {
            return new RedisLockTestService();
        }
    }

    @Service
    static class RedisLockTestService {

        private final AtomicInteger executedCount = new AtomicInteger();

        /*
         * RedisLockAspect는 첫 번째 파라미터를 lock target id로 사용한다.
         *
         * lock key:
         * lock:test-order:{lockTargetId}
         *
         * retryCount를 1로 둔 이유:
         * 테스트에서 분산락 획득 실패를 명확하게 보기 위해서다.
         * 하나의 스레드가 락을 잡고 500ms 동안 대기하는 동안,
         * 나머지 스레드는 재시도 없이 바로 LOCK_ACQUIRE_FAILED가 발생한다.
         */
        @RedisLock(key = "lock:test-order", timeout = 5, retryCount = 1, retryDelayMillis = 10)
        public void executeWithLock(Long lockTargetId) {
            executedCount.incrementAndGet();

            try {
                Thread.sleep(500);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }

        public int getExecutedCount() {
            return executedCount.get();
        }

        public void reset() {
            executedCount.set(0);
        }
    }
}
