package com.example.bori.domain.rental.controller.dto;

import com.example.bori.domain.rental.domain.Rental;
import com.example.bori.domain.rental.domain.RentalStatus;
import java.time.LocalDateTime;

// 반납 승인 목록·대여 이력 한 줄
public record AdminRentalResponse(
        Long id,
        Long boardId,
        String boardName,
        boolean boardDeleted,
        String spec,
        String userName,
        String studentNo,
        RentalStatus status,
        LocalDateTime rentedAt,
        LocalDateTime returnRequestedAt,
        LocalDateTime returnedAt
) {

    public static AdminRentalResponse from(Rental r) {
        var b = r.getRentalBoard();
        return new AdminRentalResponse(r.getId(), b.getId(), b.getName(), b.isDeleted(), b.getSpec(),
                r.getUser().getName(), r.getUser().getStudentNo(), r.getStatus(), r.getRentedAt(),
                r.getReturnRequestedAt(), r.getReturnedAt());
    }
}
