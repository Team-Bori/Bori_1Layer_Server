package com.example.bori.domain.rental.controller.dto;

import com.example.bori.domain.rental.domain.Rental;
import com.example.bori.domain.rental.domain.RentalStatus;
import java.time.LocalDateTime;

public record MyRentalResponse(
        Long id,
        Long boardId,
        String boardName,
        boolean boardDeleted,
        String accessories,
        RentalStatus status,
        LocalDateTime rentedAt,
        LocalDateTime returnRequestedAt,
        LocalDateTime returnedAt
) {

    public static MyRentalResponse from(Rental r) {
        var b = r.getRentalBoard();
        return new MyRentalResponse(r.getId(), b.getId(), b.getName(), b.isDeleted(), b.getAccessories(), r.getStatus(),
                r.getRentedAt(), r.getReturnRequestedAt(), r.getReturnedAt());
    }
}
