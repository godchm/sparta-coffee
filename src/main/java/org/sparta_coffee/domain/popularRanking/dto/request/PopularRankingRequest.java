package org.sparta_coffee.domain.popularRanking.dto.request;

import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@Builder
public record PopularRankingRequest(
        @NotBlank(message = "검색어는 필수입니다.")
        String keyword

) {
}