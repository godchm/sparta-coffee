package org.sparta_coffee.domain.user.dto.response;

import lombok.Builder;
import org.sparta_coffee.domain.user.enums.UserRole;

@Builder
public record LoginResponse(
        Long userId,
        String name,
        String email,
        UserRole role,
        String accessToken
) {
}
