package org.sparta_coffee.domain.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;


@Builder
public record OrderCreateRequest(
        @NotNull(message = "메뉴 ID는 필수입니다.")
        Long menuId
) {
}
