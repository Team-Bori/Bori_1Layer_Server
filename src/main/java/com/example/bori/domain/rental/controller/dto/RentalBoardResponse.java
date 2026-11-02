package com.example.bori.domain.rental.controller.dto;

import com.example.bori.domain.rental.domain.RentalBoard;
import com.example.bori.domain.rental.domain.RentalBoardStatus;

public record RentalBoardResponse(
        Long id,
        String name,
        String spec,
        String photoUrl,
        String accessories,
        RentalBoardStatus status
) {

    public static RentalBoardResponse from(RentalBoard b) {
        return new RentalBoardResponse(b.getId(), b.getName(), b.getSpec(), b.getPhotoUrl(), b.getAccessories(), b.getStatus());
    }
}
