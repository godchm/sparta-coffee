package org.sparta_coffee.domain.point.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.point.dto.response.PointResponse;
import org.sparta_coffee.global.dto.ApiResponseDto;
import org.sparta_coffee.domain.point.dto.request.PointChargeRequest;
import org.sparta_coffee.domain.point.dto.response.PointChargeResponse;
import org.sparta_coffee.domain.point.service.PointService;
import org.sparta_coffee.global.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController {

    private final PointService pointService;

    // 포인트 충전
    @PostMapping("/charge")
    public ResponseEntity<ApiResponseDto<PointChargeResponse>> charge(
            @Valid @RequestBody PointChargeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(
                        HttpStatus.CREATED,
                        pointService.charge(request, userDetails.userId())
                ));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<PointResponse>> getPoint(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success(
                        HttpStatus.OK,
                        pointService.getPoint(userId, userDetails.userId(), userDetails.role())
                )
        );
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<Void>> deletePoint(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        pointService.deletePoint(userId, userDetails.userId(), userDetails.role());

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }
}
