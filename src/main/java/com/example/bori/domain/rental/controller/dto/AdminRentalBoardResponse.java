package com.example.bori.domain.rental.controller.dto;

import com.example.bori.domain.rental.domain.Rental;
import com.example.bori.domain.rental.domain.RentalBoard;
import com.example.bori.domain.rental.domain.RentalBoardStatus;
import java.time.LocalDateTime;

// renter·rentedAt: 진행 중 대여(대여중·반납 신청)가 없으면 null
public record AdminRentalBoardResponse(
        Long id,
        String name,
        String spec,
        String photoUrl,
        String accessories,
        RentalBoardStatus status,
        Long rentalId,
        String renterName,
        String renterStudentNo,
        LocalDateTime rentedAt
) {

    public static AdminRentalBoardResponse of(RentalBoard b, Rental active) {
        return new AdminRentalBoardResponse(b.getId(), b.getName(), b.getSpec(), b.getPhotoUrl(), b.getAccessories(),
                b.getStatus(),
                active == null ? null : active.getId(),
                active == null ? null : active.getUser().getName(),
                active == null ? null : active.getUser().getStudentNo(),
                active == null ? null : active.getRentedAt());
    }
}
