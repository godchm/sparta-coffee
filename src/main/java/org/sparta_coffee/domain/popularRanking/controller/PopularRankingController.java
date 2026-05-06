package org.sparta_coffee.domain.popularRanking.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta_coffee.common.config.model.redis.RankingDto;
import org.sparta_coffee.domain.popularRanking.dto.request.PopularRankingRequest;
import org.sparta_coffee.domain.popularRanking.service.PopularRankingService;
import org.sparta_coffee.global.dto.ApiResponseDto;
import org.sparta_coffee.global.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/popular-ranking")
public class PopularRankingController {

    private final PopularRankingService popularRankingService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<Void>> search(
            @Valid @RequestBody PopularRankingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        popularRankingService.search(request, userDetails.userId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, null));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponseDto<List<RankingDto>>> getTodayRanking() {
        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.OK, popularRankingService.getTodayRanking())
        );
    }
}
