package org.sparta_coffee.domain.order.service;


import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.order.dto.response.OrderPaidNotificationResponse;
import org.sparta_coffee.domain.order.entity.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private static final String ADMIN_ORDER_TOPIC = "/topic/admin/orders";

    private final SimpMessagingTemplate messagingTemplate;

    public void sendOrderPaid(Order order) {
        OrderPaidNotificationResponse response = OrderPaidNotificationResponse.builder()
                .type("ORDER_PAID")
                .orderId(order.getId())
                .userId(order.getUser().getId())
                .menuId(null)
                .menuName("여러 메뉴")
                .paymentAmount(order.getPaymentAmount())
                .message(order.getUser().getId() + "번 고객이 주문 결제를 완료했습니다.")
                .build();

        messagingTemplate.convertAndSend(ADMIN_ORDER_TOPIC, response);
    }
}