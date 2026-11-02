package com.example.bori.domain.board.controller.dto;

import com.example.bori.domain.board.domain.BoardStatus;
import jakarta.validation.constraints.NotNull;

public record BoardStatusRequest(
        @NotNull(message = "상태를 선택해주세요.")
        BoardStatus status
) {
}
