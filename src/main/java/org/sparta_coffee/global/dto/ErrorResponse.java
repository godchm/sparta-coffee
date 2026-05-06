package org.sparta_coffee.global.dto;

public record ErrorResponse<T>(
        String code,
        String message,
        T data
) {

    public static <T> ErrorResponse<T> of(String code, String message, T data) {
        return new ErrorResponse<>(code, message, data);
    }

    public static ErrorResponse<Void> of(String code, String message) {
        return new ErrorResponse<>(code, message, null);
    }
}
