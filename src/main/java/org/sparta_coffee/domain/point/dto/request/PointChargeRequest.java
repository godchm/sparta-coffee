package org.sparta_coffee.domain.point.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;


@Builder
public record PointChargeRequest(

        @Min(value = 1, message = "충전 금액은 1원 이상이어야 합니다.")
        long amount
) {
}
