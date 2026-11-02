package com.example.bori.domain.rental.domain;

import java.util.List;

public enum RentalStatus {
    RENTED,
    RETURN_REQUESTED,
    RETURNED;

    public static final List<RentalStatus> ACTIVE = List.of(RENTED, RETURN_REQUESTED);
}
