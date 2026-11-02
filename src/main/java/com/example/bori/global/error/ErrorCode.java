package com.example.bori.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A003", "이메일 또는 비밀번호가 올바르지 않습니다."),

    // User
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U001", "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "사용자를 찾을 수 없습니다."),

    // Relay Pi
    PI_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "릴레이 Pi를 찾을 수 없습니다."),
    DUPLICATE_PI_SERIAL(HttpStatus.CONFLICT, "P002", "이미 등록된 Pi 시리얼입니다."),

    // Cloud board
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "보드를 찾을 수 없습니다."),
    DUPLICATE_BOARD_SERIAL(HttpStatus.CONFLICT, "B002", "이미 등록된 보드 시리얼입니다."),
    DUPLICATE_BOARD_SLOT(HttpStatus.CONFLICT, "B003", "해당 Pi의 USB 슬롯에 이미 다른 보드가 있습니다."),

    // AI model
    MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "모델을 찾을 수 없습니다."),
    INVALID_GITHUB_URL(HttpStatus.BAD_REQUEST, "M002", "https://github.com/사용자/저장소 형식이어야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
