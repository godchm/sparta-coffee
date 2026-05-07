package org.sparta_coffee.domain.order.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta_coffee.domain.menu.entity.Menu;
import org.sparta_coffee.domain.menu.repository.MenuRepository;
import org.sparta_coffee.domain.order.dto.response.OrderPayResponse;
import org.sparta_coffee.domain.order.entity.Order;
import org.sparta_coffee.domain.order.entity.OrderStatus;
import org.sparta_coffee.domain.order.repository.OrderRepository;
import org.sparta_coffee.domain.point.entity.UserPoint;
import org.sparta_coffee.domain.point.repository.UserPointRepository;
import org.sparta_coffee.domain.user.enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class OrderDistributedLockTest {


    // 결제 동시성 검증 = 최종적으로 결제가 한 번만 됐는지
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("같은 주문에 동시에 결제를 요청하면 Redis 분산락으로 하나만 성공한다")
    void payOrderWithDistributedLock() throws InterruptedException {
        // given
        Long userId = 1L;

        Menu menu = menuRepository.save(
                Menu.builder()
                        .name("락 테스트 아메리카노")
                        .price(3000)
                        .build()
        );

        UserPoint userPoint = UserPoint.builder()
                .userId(userId)
                .balance(10000)
                .build();

        userPointRepository.save(userPoint);

        Order order = Order.builder()
                .userId(userId)
                .paymentAmount(menu.getPrice())
                .status(OrderStatus.PENDING)
                .orderedAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // 성공/실패 개수 확인용
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger();

        // 혹시 이전 테스트 lock key가 남아 있으면 제거
        stringRedisTemplate.delete("lock:order:" + savedOrder.getId());

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    OrderPayResponse response = orderService.payOrder(
                            savedOrder.getId(),
                            userId,
                            UserRole.USER
                    );

                    if (response != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
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

        // then
        Order paidOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        UserPoint resultPoint = userPointRepository.findByUserId(userId).orElseThrow();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(resultPoint.getBalance()).isEqualTo(7000);

        assertThat(stringRedisTemplate.hasKey("lock:order:" + savedOrder.getId())).isFalse();
    }
}