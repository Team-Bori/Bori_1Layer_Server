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
    CONFLICT(HttpStatus.CONFLICT, "C005", "다른 요청과 충돌했습니다. 다시 시도해주세요."),

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
    BOARD_UNAVAILABLE(HttpStatus.CONFLICT, "B004", "지금은 이 보드에 접속할 수 없습니다."),
    BOARD_BUSY(HttpStatus.CONFLICT, "B005", "다른 사용자가 사용 중인 보드입니다."),
    BOARD_RESERVED(HttpStatus.CONFLICT, "B006", "대기열의 다른 사용자 차례입니다."),

    // AI model
    MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "모델을 찾을 수 없습니다."),
    INVALID_GITHUB_URL(HttpStatus.BAD_REQUEST, "M002", "https://github.com/사용자/저장소 형식이어야 합니다."),
    MODEL_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "M003", "이 보드에서 지원하지 않는 모델입니다."),
    UPLOAD_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "M004", "보드의 업로드 한도를 넘습니다."),

    // Session
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "세션을 찾을 수 없습니다."),
    SESSION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "S002", "이미 실행 중인 세션이 있습니다. 한 번에 하나의 보드만 사용할 수 있습니다."),
    SESSION_ALREADY_ENDED(HttpStatus.CONFLICT, "S003", "이미 끝난 세션입니다."),
    INVALID_MODEL_SOURCE(HttpStatus.BAD_REQUEST, "S004", "검증 모델 또는 GitHub 링크(형식·크기 포함) 중 하나를 입력해주세요."),

    // Queue
    QUEUE_NOT_NEEDED(HttpStatus.CONFLICT, "Q001", "지금 바로 접속할 수 있는 보드입니다."),
    QUEUE_FULL(HttpStatus.CONFLICT, "Q002", "대기열이 가득 찼습니다."),
    ALREADY_QUEUED(HttpStatus.CONFLICT, "Q003", "이미 대기열에 등록되어 있습니다. 대기는 한 번에 하나만 가능합니다."),
    QUEUE_NOT_FOUND(HttpStatus.NOT_FOUND, "Q004", "대기 중인 항목이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
