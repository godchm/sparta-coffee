package org.sparta_coffee.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderUpdateRequest(

        @Valid
        @NotEmpty(message = "주문 메뉴는 1개 이상이어야 합니다.")
        List<OrderItemRequest> items
) {
}
