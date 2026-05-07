package org.sparta_coffee.domain.order.service;

import lombok.RequiredArgsConstructor;
import org.sparta_coffee.common.config.annotation.RedisLock;
import org.sparta_coffee.domain.menu.entity.Menu;
import org.sparta_coffee.domain.menu.repository.MenuRepository;
import org.sparta_coffee.domain.order.dto.request.OrderCreateRequest;
import org.sparta_coffee.domain.order.dto.request.OrderUpdateRequest;
import org.sparta_coffee.domain.order.dto.response.OrderPayResponse;
import org.sparta_coffee.domain.order.dto.response.OrderResponse;
import org.sparta_coffee.domain.order.entity.Order;
import org.sparta_coffee.domain.order.entity.OrderStatus;
import org.sparta_coffee.domain.order.repository.OrderRepository;
import org.sparta_coffee.domain.point.service.PointService;
import org.sparta_coffee.domain.user.enums.UserRole;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.MenuException;
import org.sparta_coffee.global.exception.domain.OrderException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final PointService pointService;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, Long loginUserId) {
        Menu menu = menuRepository.findById(request.menuId())
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));

        Order order = Order.builder()
                .userId(loginUserId)
                .menu(menu)
                .paymentAmount(menu.getPrice())
                .status(OrderStatus.PENDING)
                .orderedAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, Long loginUserId, UserRole loginUserRole) {
        Order order = findOrder(orderId);

        validateOwnerOrAdmin(order, loginUserId, loginUserRole);

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateOrder(Long orderId, OrderUpdateRequest request, Long loginUserId, UserRole loginUserRole) {
        Order order = findOrder(orderId);

        validateOwnerOrAdmin(order, loginUserId, loginUserRole);
        validatePending(order);

        Menu menu = menuRepository.findById(request.menuId())
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));

        order.updateMenu(menu);

        return OrderResponse.from(order);
    }



    @Transactional
    public void cancelOrder(Long orderId, Long loginUserId, UserRole loginUserRole) {
        Order order = findOrder(orderId);

        validateOwnerOrAdmin(order, loginUserId, loginUserRole);

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new OrderException(ErrorCode.ORDER_ALREADY_CANCELED);
        }

        if (order.getStatus() == OrderStatus.PAID) {
            pointService.refund(order.getUserId(), order.getPaymentAmount());
        }

        order.cancel();
    }


    // 결제 진행
    // 분산락 적용.
    @RedisLock(key = "lock:order", timeout = 60, retryCount = 3, retryDelayMillis = 100)
    @Transactional
    public OrderPayResponse payOrder(Long orderId, Long loginUserId, UserRole loginUserRole) {
        Order order = findOrder(orderId);

        validateOwnerOrAdmin(order, loginUserId, loginUserRole);
        validatePending(order);

        long remainingPoint = pointService.use(order.getUserId(), order.getPaymentAmount());

        order.pay();

        return OrderPayResponse.from(order, remainingPoint);
    }

    // 주문 찾기
    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));
    }

    // 본인이나 관리자만 가능.
    private void validateOwnerOrAdmin(Order order, Long loginUserId, UserRole loginUserRole) {
        if (loginUserRole == UserRole.ADMIN) {
            return;
        }

        if (!order.getUserId().equals(loginUserId)) {
            throw new OrderException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validatePending(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderException(ErrorCode.ORDER_CANNOT_UPDATE);
        }
    }
}
