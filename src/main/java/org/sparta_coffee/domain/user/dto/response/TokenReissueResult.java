package org.sparta_coffee.domain.user.dto.response;

import lombok.Builder;

@Builder
public record TokenReissueResult(
        TokenReissueResponse tokenReissueResponse,
        String refreshToken
) {
}