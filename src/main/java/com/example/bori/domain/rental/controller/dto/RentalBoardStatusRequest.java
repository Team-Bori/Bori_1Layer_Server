package com.example.bori.domain.rental.controller.dto;

import com.example.bori.domain.rental.domain.RentalBoardStatus;
import jakarta.validation.constraints.NotNull;

public record RentalBoardStatusRequest(
        @NotNull(message = "상태를 선택해주세요.")
        RentalBoardStatus status
) {
}
