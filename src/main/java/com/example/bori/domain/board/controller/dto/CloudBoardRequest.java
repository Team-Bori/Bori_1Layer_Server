package com.example.bori.domain.board.controller.dto;

import com.example.bori.domain.board.domain.BoardType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CloudBoardRequest(
        @NotBlank(message = "보드명을 입력해주세요.")
        @Size(max = 100, message = "보드명은 100자 이하여야 합니다.")
        String name,

        @NotNull(message = "사양을 입력해주세요.")
        String spec,

        @NotNull(message = "보드 종류를 선택해주세요.")
        BoardType boardType,

        @NotBlank(message = "호스트를 입력해주세요.")
        @Size(max = 255, message = "호스트는 255자 이하여야 합니다.")
        String host,

        @NotNull(message = "릴레이 Pi를 선택해주세요.")
        Long piId,

        @NotBlank(message = "USB 슬롯을 입력해주세요.")
        @Size(max = 32, message = "USB 슬롯은 32자 이하여야 합니다.")
        String usbSlot,

        @NotBlank(message = "시리얼 번호를 입력해주세요.")
        @Size(max = 128, message = "시리얼 번호는 128자 이하여야 합니다.")
        String serialCode,

        @NotNull(message = "업로드 한도를 입력해주세요.")
        @Min(value = 0, message = "업로드 한도는 0 이상이어야 합니다.")
        @Max(value = 100_000, message = "업로드 한도가 너무 큽니다.")
        Integer uploadLimitMb,

        boolean executeAble,

        List<Long> modelIds
) {
}
