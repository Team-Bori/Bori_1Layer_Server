package com.example.bori.domain.pi.controller.dto;

import com.example.bori.domain.pi.domain.HostPi;
import java.time.LocalDateTime;

public record HostPiResponse(
        Long id,
        String name,
        String serialCode,
        LocalDateTime lastUsedAt
) {

    public static HostPiResponse from(HostPi pi) {
        return new HostPiResponse(pi.getId(), pi.getName(), pi.getSerialCode(), pi.getLastUsedAt());
    }
}
