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
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "사용자를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
