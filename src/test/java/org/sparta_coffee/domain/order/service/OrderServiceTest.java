package org.sparta_coffee.domain.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sparta_coffee.common.config.model.kafka.event.PopularRankingEvent;
import org.sparta_coffee.domain.menu.entity.Menu;
import org.sparta_coffee.domain.menu.repository.MenuRepository;
import org.sparta_coffee.domain.order.dto.request.OrderCreateRequest;
import org.sparta_coffee.domain.order.dto.request.OrderItemRequest;
import org.sparta_coffee.domain.order.dto.response.OrderPayResponse;
import org.sparta_coffee.domain.order.dto.response.OrderResponse;
import org.sparta_coffee.domain.order.entity.Order;
import org.sparta_coffee.domain.order.entity.OrderItem;
import org.sparta_coffee.domain.order.entity.OrderStatus;
import org.sparta_coffee.domain.order.repository.OrderItemRepository;
import org.sparta_coffee.domain.order.repository.OrderRepository;
import org.sparta_coffee.domain.point.service.PointService;
import org.sparta_coffee.domain.popularRanking.entity.PendingPopularRankingEvent;
import org.sparta_coffee.domain.popularRanking.producer.PopularRankingProducer;
import org.sparta_coffee.domain.popularRanking.repository.PendingPopularRankingEventRepository;
import org.sparta_coffee.domain.user.entity.User;
import org.sparta_coffee.domain.user.enums.UserRole;
import org.sparta_coffee.domain.user.repository.UserRepository;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.PointException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private PointService pointService;

    @Mock
    private PopularRankingProducer popularRankingProducer;

    @Mock
    private PendingPopularRankingEventRepository pendingPopularRankingEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("복수 메뉴 주문 생성에 성공한다")
    void createOrderWithMultipleMenus() {
        /*
         * given
         */
        User user = createUser(1L);
        Menu americano = createMenu(1L, "아메리카노", 3000);
        Menu latte = createMenu(2L, "카페라떼", 3500);

        OrderCreateRequest request = OrderCreateRequest.builder()
                .items(List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 1)
                ))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(menuRepository.findById(1L)).thenReturn(Optional.of(americano));
        when(menuRepository.findById(2L)).thenReturn(Optional.of(latte));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            setId(order, 10L);
            return order;
        });

        /*
         * when
         */
        OrderResponse response = orderService.createOrder(request, 1L);

        /*
         * then
         */
        assertThat(response.orderId()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.paymentAmount()).isEqualTo(9500);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(response.items()).hasSize(2);

        assertThat(response.items().get(0).menuName()).isEqualTo("아메리카노");
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.items().get(0).subtotalAmount()).isEqualTo(6000);

        assertThat(response.items().get(1).menuName()).isEqualTo("카페라떼");
        assertThat(response.items().get(1).quantity()).isEqualTo(1);
        assertThat(response.items().get(1).subtotalAmount()).isEqualTo(3500);

        ArgumentCaptor<List<OrderItem>> orderItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(orderItemsCaptor.capture());

        List<OrderItem> savedItems = orderItemsCaptor.getValue();
        assertThat(savedItems).hasSize(2);
        assertThat(savedItems.get(0).getOrderId()).isEqualTo(10L);
        assertThat(savedItems.get(1).getOrderId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("포인트로 주문 결제에 성공한다")
    void payOrder() throws Exception {
        /*
         * given
         */
        User user = createUser(1L);
        Order order = createOrder(10L, user, 9500, OrderStatus.PENDING);

        List<OrderItem> items = List.of(
                new OrderItem(order, createMenu(1L, "아메리카노", 3000), 2),
                new OrderItem(order, createMenu(2L, "카페라떼", 3500), 1)
        );

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrder_Id(10L)).thenReturn(items);
        when(pointService.use(1L, 9500)).thenReturn(90500L);
        when(objectMapper.writeValueAsString(any(PopularRankingEvent.class))).thenReturn("{}");
        when(pendingPopularRankingEventRepository.save(any(PendingPopularRankingEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        /*
         * when
         */
        OrderPayResponse response = orderService.payOrder(10L, 1L, UserRole.USER);

        /*
         * then
         */
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.order().orderId()).isEqualTo(10L);
        assertThat(response.order().status()).isEqualTo(OrderStatus.PAID.name());
        assertThat(response.order().paymentAmount()).isEqualTo(9500);
        assertThat(response.order().items()).hasSize(2);
        assertThat(response.remainingPoint()).isEqualTo(90500);

        verify(pointService).use(1L, 9500);
        verify(pendingPopularRankingEventRepository, times(2))
                .save(any(PendingPopularRankingEvent.class));
    }

    @Test
    @DisplayName("포인트가 부족하면 주문 결제에 실패한다")
    void payOrderFailsWhenPointIsInsufficient() {
        /*
         * given
         */
        User user = createUser(1L);
        Order order = createOrder(10L, user, 9500, OrderStatus.PENDING);

        List<OrderItem> items = List.of(
                new OrderItem(order, createMenu(1L, "아메리카노", 3000), 2),
                new OrderItem(order, createMenu(2L, "카페라떼", 3500), 1)
        );

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrder_Id(10L)).thenReturn(items);
        when(pointService.use(1L, 9500))
                .thenThrow(new PointException(ErrorCode.INSUFFICIENT_POINT));

        /*
         * when & then
         */
        assertThatThrownBy(() -> orderService.payOrder(10L, 1L, UserRole.USER))
                .isInstanceOf(PointException.class)
                .satisfies(exception -> {
                    PointException pointException = (PointException) exception;
                    assertThat(pointException.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_POINT);
                });

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        verify(pointService).use(1L, 9500);
        verify(pendingPopularRankingEventRepository, never()).save(any(PendingPopularRankingEvent.class));
    }

    private User createUser(Long userId) {
        User user = User.builder()
                .name("주문 테스트 유저")
                .email("order-test@example.com")
                .password("password")
                .role(UserRole.USER)
                .build();

        setId(user, userId);

        return user;
    }

    private Menu createMenu(Long menuId, String name, long price) {
        Menu menu = Menu.builder()
                .name(name)
                .price(price)
                .build();

        setId(menu, menuId);

        return menu;
    }

    private Order createOrder(Long orderId, User user, long paymentAmount, OrderStatus status) {
        Order order = Order.builder()
                .user(user)
                .paymentAmount(paymentAmount)
                .status(status)
                .orderedAt(LocalDateTime.now())
                .build();

        setId(order, orderId);

        return order;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
