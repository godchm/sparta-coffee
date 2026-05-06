package org.sparta_coffee.domain.point.dto.response;


import lombok.Builder;

@Builder
public record PointChargeResponse(
        Long userId,
        long chargedAmount,
        long balance
) {
}
