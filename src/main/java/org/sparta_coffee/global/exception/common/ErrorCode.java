package org.sparta_coffee.global.exception.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400_001", "입력값이 올바르지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON_400_002", "필수 요청 파라미터가 누락되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401_001", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_403_001", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500_001", "서버 내부 오류가 발생했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_001", "유저를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_400_001", "비밀번호가 일치하지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_409_001", "이미 사용 중인 이메일입니다."),

    // Auth
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_001", "토큰이 만료되었습니다."),
    TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "AUTH_401_002", "유효하지 않은 토큰입니다."),
    TOKEN_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_401_003", "토큰 서명이 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_004", "유효하지 않은 토큰입니다."),


    // Menu
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "MENU_404_001", "메뉴를 찾을 수 없습니다."),

    // Point
    POINT_NOT_FOUND(HttpStatus.NOT_FOUND, "POINT_404_001", "포인트 정보를 찾을 수 없습니다."),
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "POINT_400_001", "충전 금액이 올바르지 않습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "POINT_400_002", "포인트 잔액이 부족합니다."),
    CONFLICT_POINT_UPDATE(HttpStatus.CONFLICT, "POINT_409_001", "포인트 처리 중 충돌이 발생했습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_404_001", "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_PAID(HttpStatus.BAD_REQUEST, "ORDER_400_001", "이미 결제 완료된 주문입니다."),
    ORDER_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "ORDER_400_002", "이미 취소된 주문입니다."),
    ORDER_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "ORDER_400_003", "취소할 수 없는 주문 상태입니다."),
    ORDER_CANNOT_UPDATE(HttpStatus.BAD_REQUEST, "ORDER_400_004", "결제 전 주문만 수정할 수 있습니다."),


    // Popular
    POPULAR_MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "POPULAR_404_001", "인기 메뉴 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
