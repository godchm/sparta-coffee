package org.sparta_coffee.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;


@Builder
public record OrderCreateRequest(
        @Valid
        @NotEmpty(message = "주문 메뉴는 1개 이상이어야 합니다.")
        List<OrderItemRequest> items
) {
}
