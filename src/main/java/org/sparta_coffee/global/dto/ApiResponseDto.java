package org.sparta_coffee.global.dto;

import org.springframework.http.HttpStatus;

public record ApiResponseDto<T>(
        boolean success,
        int status,
        T data
) {

    public static <T> ApiResponseDto<T> success(HttpStatus status, T data) {
        return new ApiResponseDto<>(true, status.value(), data);
    }

    public static ApiResponseDto<Void> successWithNoContent() {
        return new ApiResponseDto<>(true, HttpStatus.NO_CONTENT.value(), null);
    }
}
