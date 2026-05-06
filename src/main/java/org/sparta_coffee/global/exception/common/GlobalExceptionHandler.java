package org.sparta_coffee.global.exception.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.sparta_coffee.global.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스 계층에서 의도적으로 발생시킨 예외를 처리합니다.
    // 각 도메인 예외(MenuException, PointException 등)는 ServiceException을 상속하므로 여기로 모입니다.
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse<Void>> handleServiceException(ServiceException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return createErrorResponse(errorCode, exception.getMessage());
    }

    // @Valid 검증에 실패했을 때 발생하는 예외를 처리합니다.
    // 어떤 필드가 왜 실패했는지 클라이언트가 알 수 있도록 data에 필드별 메시지를 담습니다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        Map<String, String> errors = new LinkedHashMap<>();

        // request DTO 필드 단위 검증 실패 메시지입니다. 예: amount -> 충전 금액은 1원 이상이어야 합니다.
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        // 특정 필드가 아니라 객체 전체 조건 검증에서 실패한 메시지입니다.
        exception.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));

        return createErrorResponse(errorCode, errors);
    }

    // @RequestParam 같은 필수 요청 파라미터가 빠졌을 때 발생하는 예외를 처리합니다.
    // 누락된 파라미터 이름과 타입을 data에 담아 디버깅하기 쉽게 만듭니다.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse<Map<String, String>>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception
    ) {
        ErrorCode errorCode = ErrorCode.MISSING_REQUEST_PARAMETER;
        Map<String, String> data = Map.of(
                "parameterName", exception.getParameterName(),
                "parameterType", exception.getParameterType()
        );

        return createErrorResponse(errorCode, data);
    }

    // 인증은 되었지만 해당 API에 접근할 권한이 없을 때 발생하는 예외를 처리합니다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        return createErrorResponse(ErrorCode.ACCESS_DENIED);
    }

    // 로그인하지 않았거나 인증 정보가 유효하지 않을 때 발생하는 예외를 처리합니다.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse<Void>> handleAuthenticationException(AuthenticationException exception) {
        return createErrorResponse(ErrorCode.UNAUTHORIZED);
    }

    // 위에서 처리하지 못한 모든 예외를 마지막으로 잡습니다.
    // 예상하지 못한 내부 오류이므로 클라이언트에는 공통 서버 에러 메시지만 내려줍니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<Void>> handleException(Exception exception) {
        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // data가 없는 실패 응답을 만들 때 사용하는 헬퍼입니다.
    private ResponseEntity<ErrorResponse<Void>> createErrorResponse(ErrorCode errorCode) {
        return createErrorResponse(errorCode, errorCode.getMessage());
    }

    // ServiceException처럼 기본 메시지 대신 예외에서 전달한 메시지를 내려야 할 때 사용합니다.
    private ResponseEntity<ErrorResponse<Void>> createErrorResponse(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode.getCode(), message));
    }

    // validation error처럼 data에 추가 정보를 담아야 할 때 사용하는 헬퍼입니다.
    private <T> ResponseEntity<ErrorResponse<T>> createErrorResponse(ErrorCode errorCode, T data) {
        HttpStatus status = errorCode.getStatus();
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), data));
    }
}
