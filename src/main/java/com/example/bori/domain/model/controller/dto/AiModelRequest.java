package com.example.bori.domain.model.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiModelRequest(
        @NotBlank(message = "모델명을 입력해주세요.")
        @Size(max = 100, message = "모델명은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "모델 형식을 입력해주세요.")
        @Size(max = 30, message = "모델 형식은 30자 이하여야 합니다.")
        String format,

        String description,

        @NotBlank(message = "저장소 링크를 입력해주세요.")
        @Size(max = 500, message = "저장소 링크는 500자 이하여야 합니다.")
        String repoUrl
) {
}
