package com.example.bori.global.error;

import java.util.List;
import org.springframework.validation.BindingResult;

public record ErrorResponse(
        String code,
        String message,
        List<FieldError> errors
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        List<FieldError> errors = bindingResult.getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errors);
    }

    public record FieldError(
            String field,
            String reason
    ) {
    }
}
