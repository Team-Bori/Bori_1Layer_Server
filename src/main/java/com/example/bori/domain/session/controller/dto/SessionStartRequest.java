package com.example.bori.domain.session.controller.dto;

import jakarta.validation.constraints.Size;

// 검증 모델이면 aiModelId만, 자체 학습 모델이면 githubUrl·modelFormat·sizeMb.
public record SessionStartRequest(
        Long aiModelId,

        @Size(max = 500, message = "GitHub 링크는 500자 이하여야 합니다.")
        String githubUrl,

        @Size(max = 30, message = "모델 형식은 30자 이하여야 합니다.")
        String modelFormat,

        Integer sizeMb
) {

    public boolean isVerified() {
        return aiModelId != null && githubUrl == null;
    }

    public boolean isUpload() {
        return aiModelId == null && githubUrl != null && modelFormat != null && !modelFormat.isBlank()
                && sizeMb != null && sizeMb > 0;
    }
}
