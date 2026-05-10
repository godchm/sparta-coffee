package org.sparta_coffee.domain.user.dto.response;

import lombok.Builder;

@Builder
public record LoginResult(
        LoginResponse loginResponse,
        String refreshToken
) {
}