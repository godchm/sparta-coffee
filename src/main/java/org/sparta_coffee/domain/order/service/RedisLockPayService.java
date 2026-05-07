package org.sparta_coffee.domain.order.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockPayService {

    private final StringRedisTemplate redisTemplate;

    // Redis에 락 key를 생성해서 락 획득을 시도한다.
    // setIfAbsent는 Redis SET NX와 같은 역할을 한다.
    // key가 없으면 저장 후 true, 이미 있으면 false를 반환한다.
    public boolean tryLock(String key, String value, long timeoutSeconds) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, value, Duration.ofSeconds(timeoutSeconds));

        return Boolean.TRUE.equals(result);
    }

    // 락을 해제한다.
    // Lua script를 사용해서 "내가 획득한 락"일 때만 삭제한다.
    // 다른 요청이 만든 락을 실수로 삭제하는 문제를 막기 위함이다.
    public void unlock(String key, String value) {


        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "   return redis.call('del', KEYS[1]) " +
                        "else " +
                        "   return 0 " +
                        "end";

        redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key),
                value
        );
        log.info("획득한 락 키 반납 :::: {}, UUID == {}", Thread.currentThread().getName(), value);

    }



}
