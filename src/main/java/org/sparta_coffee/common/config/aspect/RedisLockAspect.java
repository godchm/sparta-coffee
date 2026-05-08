package org.sparta_coffee.common.config.aspect;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sparta_coffee.common.config.annotation.RedisLock;
import org.sparta_coffee.domain.order.service.RedisLockPayService;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.OrderException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisLockAspect {

    private final RedisLockPayService redisLockPayService;

    @Around("@annotation(redisLock)")
    public Object run(ProceedingJoinPoint joinPoint,
                      RedisLock redisLock) throws Throwable {

        String keyPreFix = redisLock.key();

        // 락 소유자를 구분하기 위한 값.
        // unlock 시 내가 획득한 락인지 검증하는 데 사용한다.
        String value = UUID.randomUUID().toString();

        Object[] args = joinPoint.getArgs();

        // 현재 구현은 어노테이션이 붙은 메서드의 첫 번째 파라미터를 락 대상 ID로 사용한다.
        // 예: payOrder(Long orderId, ...) -> lock:order:{orderId}
        Object lockTargetId = args[0];
        String key = keyPreFix + ":" +lockTargetId;


       // 재시도 로직
       // Redis에 같은 key가 없으면 락 획득 성공.
       // 이미 같은 key가 있으면 다른 요청이 처리 중이므로 락 획득 실패.
       // retryCount 횟수만큼 락 획득을 재시도한다.
        boolean locked = false;

        for (int attempt = 1; attempt <= redisLock.retryCount(); attempt++) {
            locked = redisLockPayService.tryLock(key, value, redisLock.timeout());

            if (locked) {
                log.info(
                        "Redis 분산락 획득 성공: key={}, attempt={}, thread={}",
                        key,
                        attempt,
                        Thread.currentThread().getName()
                );
                break;
            }

            log.info(
                    "Redis 분산락 획득 실패: key={}, attempt={}/{}, thread={}",
                    key,
                    attempt,
                    redisLock.retryCount(),
                    Thread.currentThread().getName()
            );

            // 마지막 시도가 아니라면 잠시 대기 후 다시 시도한다.
            if (attempt < redisLock.retryCount()) {
                try {
                    Thread.sleep(redisLock.retryDelayMillis());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new OrderException(ErrorCode.LOCK_INTERRUPTED);
                }
            }
        }



        if (!locked) {
            log.info("락획득 실패 : {}", Thread.currentThread().getName());
            throw new OrderException(ErrorCode.LOCK_ACQUIRE_FAILED);
        }

        // locked가 true -> 락을 획득한 경우에 실행
        try {
            log.info("락획득 성공 : {}, UUID : {} ", Thread.currentThread().getName(),value);


            // RedisLock 어노테이션이 붙은 실제 메서드를 실행한다.
            // 예: OrderService.payOrder()
            return joinPoint.proceed();
        } finally {
            // 실제 메서드 실행이 끝나면 Redis 락을 해제한다.
            redisLockPayService.unlock(key, value);
        }
    }
}
