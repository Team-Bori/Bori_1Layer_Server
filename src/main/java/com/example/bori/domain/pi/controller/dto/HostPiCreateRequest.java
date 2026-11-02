package com.example.bori.domain.pi.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HostPiCreateRequest(
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 128, message = "이름은 128자 이하여야 합니다.")
        String name,

        @NotBlank(message = "시리얼 번호를 입력해주세요.")
        @Size(max = 128, message = "시리얼 번호는 128자 이하여야 합니다.")
        String serialCode
) {
}
