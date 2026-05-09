package org.sparta_coffee.domain.order.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta_coffee.common.config.annotation.RedisLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RedisLockReleaseTest {

    @Autowired
    private RedisLockReleaseTestService redisLockReleaseTestService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("락을 획득한 메서드에서 예외가 발생해도 Redis lock key는 반납된다")
    void redisLockIsReleasedEvenWhenBusinessLogicThrowsException() {
        /*
         * given
         *
         * 락을 획득한 뒤 비즈니스 로직에서 예외가 발생하는 상황을 만든다.
         * 실제 결제 로직에서도 포인트 차감, 주문 상태 변경, 이벤트 저장 중 예외가 발생할 수 있으므로
         * 예외 상황에서도 락이 반드시 반납되는지 확인한다.
         */
        Long lockTargetId = 1L;
        String lockKey = "lock:test-release:" + lockTargetId;

        stringRedisTemplate.delete(lockKey);
        redisLockReleaseTestService.reset();

        /*
         * when & then
         *
         * 메서드 내부에서 RuntimeException이 발생하더라도
         * RedisLockAspect의 finally 블록에서 unlock이 실행되어야 한다.
         */
        assertThatThrownBy(() -> redisLockReleaseTestService.executeWithLockAndThrow(lockTargetId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("테스트 예외");

        assertThat(redisLockReleaseTestService.getExecutedCount()).isEqualTo(1);
        assertThat(stringRedisTemplate.hasKey(lockKey)).isFalse();
    }

    @Test
    @DisplayName("락이 반납된 뒤 같은 key로 다시 요청하면 정상적으로 실행된다")
    void sameKeyRequestCanExecuteAgainAfterLockReleased() {
        /*
         * given
         *
         * 같은 lockTargetId를 사용하는 요청을 순차적으로 두 번 실행한다.
         * 첫 번째 요청이 끝난 뒤 lock key가 반납되면
         * 두 번째 요청도 정상적으로 락을 획득해야 한다.
         */
        Long lockTargetId = 2L;
        String lockKey = "lock:test-release:" + lockTargetId;

        stringRedisTemplate.delete(lockKey);
        redisLockReleaseTestService.reset();

        /*
         * when
         */
        redisLockReleaseTestService.executeWithLock(lockTargetId);
        assertThat(stringRedisTemplate.hasKey(lockKey)).isFalse();

        redisLockReleaseTestService.executeWithLock(lockTargetId);

        /*
         * then
         *
         * 같은 key라도 이전 요청이 락을 정상 반납했다면
         * 다음 요청은 다시 실행될 수 있어야 한다.
         */
        assertThat(redisLockReleaseTestService.getExecutedCount()).isEqualTo(2);
        assertThat(stringRedisTemplate.hasKey(lockKey)).isFalse();
    }

    @TestConfiguration
    static class RedisLockReleaseTestConfig {

        @Bean
        public RedisLockReleaseTestService redisLockReleaseTestService() {
            return new RedisLockReleaseTestService();
        }
    }

    static class RedisLockReleaseTestService {

        private final AtomicInteger executedCount = new AtomicInteger();

        @RedisLock(key = "lock:test-release", timeout = 5, retryCount = 1, retryDelayMillis = 10)
        public void executeWithLock(Long lockTargetId) {
            executedCount.incrementAndGet();
        }

        @RedisLock(key = "lock:test-release", timeout = 5, retryCount = 1, retryDelayMillis = 10)
        public void executeWithLockAndThrow(Long lockTargetId) {
            executedCount.incrementAndGet();
            throw new IllegalStateException("테스트 예외");
        }

        public int getExecutedCount() {
            return executedCount.get();
        }

        public void reset() {
            executedCount.set(0);
        }
    }
}
