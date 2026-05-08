package org.sparta_coffee.domain.point.dto.response;

import lombok.Builder;
import org.sparta_coffee.domain.point.entity.UserPoint;

@Builder
public record PointResponse(
        Long userId,
        long balance
) {

    public static PointResponse from(UserPoint userPoint) {
        return PointResponse.builder()
                .userId(userPoint.getUser().getId())
                .balance(userPoint.getBalance())
                .build();
    }
}
