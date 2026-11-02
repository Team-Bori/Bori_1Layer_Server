package com.example.bori.domain.rental.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RentalBoardRequest(
        @NotBlank(message = "보드명을 입력해주세요.")
        @Size(max = 100, message = "보드명은 100자 이하여야 합니다.")
        String name,

        @NotNull(message = "사양을 입력해주세요.")
        String spec,

        @Size(max = 500, message = "구성품은 500자 이하여야 합니다.")
        String accessories
) {
}
