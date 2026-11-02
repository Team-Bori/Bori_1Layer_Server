package com.example.bori.domain.board.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UploadLimitRequest(
        @NotNull(message = "업로드 한도를 입력해주세요.")
        @Min(value = 0, message = "업로드 한도는 0 이상이어야 합니다.")
        @Max(value = 100_000, message = "업로드 한도가 너무 큽니다.")
        Integer uploadLimitMb
) {
}
