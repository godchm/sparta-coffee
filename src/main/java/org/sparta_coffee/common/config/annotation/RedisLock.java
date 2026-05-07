package org.sparta_coffee.common.config.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {
    String key();
    long timeout() default 30;

    // 락 획득 재시도 횟수.
    int retryCount() default 3;

    // 락 획득 실패 후 다음 재시도까지 대기 시간. 밀리초 단위.
    long retryDelayMillis() default 100;


}
