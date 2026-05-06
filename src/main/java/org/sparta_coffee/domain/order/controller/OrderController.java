package org.sparta_coffee.domain.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.order.dto.request.OrderUpdateRequest;
import org.sparta_coffee.domain.order.dto.response.OrderPayResponse;
import org.sparta_coffee.global.dto.ApiResponseDto;
import org.sparta_coffee.domain.order.dto.request.OrderCreateRequest;
import org.sparta_coffee.domain.order.dto.response.OrderResponse;
import org.sparta_coffee.domain.order.service.OrderService;
import org.sparta_coffee.global.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping("/v1/orders")
    public ResponseEntity<ApiResponseDto<OrderResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(
                        HttpStatus.CREATED,
                        orderService.createOrder(request, userDetails.userId())
                ));
    }

    // 결제
    @PostMapping("/v1/orders/{orderId}/pay")
    public ResponseEntity<ApiResponseDto<OrderPayResponse>> payOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success(
                        HttpStatus.OK,
                        orderService.payOrder(orderId, userDetails.userId(), userDetails.role())
                )
        );
    }

    @GetMapping("/v1/orders/{orderId}")
    public ResponseEntity<ApiResponseDto<OrderResponse>> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success(
                        HttpStatus.OK,
                        orderService.getOrder(orderId, userDetails.userId(), userDetails.role())
                )
        );
    }

    @PatchMapping("/v1/orders/{orderId}")
    public ResponseEntity<ApiResponseDto<OrderResponse>> updateOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success(
                        HttpStatus.OK,
                        orderService.updateOrder(orderId, request, userDetails.userId(), userDetails.role())
                )
        );
    }

    // 주문 삭제도, 취소 상태로 주문 상태 변경
    @DeleteMapping("/v1/orders/{orderId}")
    public ResponseEntity<ApiResponseDto<Void>> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        orderService.cancelOrder(orderId, userDetails.userId(), userDetails.role());

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }


}
