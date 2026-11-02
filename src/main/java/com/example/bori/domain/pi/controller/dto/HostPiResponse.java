package com.example.bori.domain.pi.controller.dto;

import com.example.bori.domain.pi.domain.HostPi;
import java.time.LocalDateTime;

public record HostPiResponse(
        Long id,
        String name,
        String serialCode,
        boolean connected,
        LocalDateTime lastUsedAt
) {

    public static HostPiResponse of(HostPi pi, boolean connected) {
        return new HostPiResponse(pi.getId(), pi.getName(), pi.getSerialCode(), connected, pi.getLastUsedAt());
    }
}
