package org.sparta_coffee.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.user.dto.request.UserCreateRequest;
import org.sparta_coffee.domain.user.dto.request.UserUpdateRequest;
import org.sparta_coffee.domain.user.dto.response.UserResponse;
import org.sparta_coffee.domain.user.service.UserService;
import org.sparta_coffee.global.dto.ApiResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    // 유저 생성
    @PostMapping
    public ResponseEntity<ApiResponseDto<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, userService.createUser(request)));
    }

    // 유저 정보 조회
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<UserResponse>> getUser(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.OK, userService.getUser(userId))
        );
    }

    // 유저 정보 수정
    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<UserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.OK, userService.updateUser(userId, request))
        );
    }

    // 유저 삭제
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteUser(
            @PathVariable Long userId
    ) {
        userService.deleteUser(userId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }
}