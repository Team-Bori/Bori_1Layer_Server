package com.example.bori.domain.pi.controller.dto;

// token은 이 응답에서만 원문으로 나간다. 다시 볼 수 없으니 L2 설정에 바로 넣어야 한다.
public record HostPiTokenResponse(
        Long id,
        String name,
        String serialCode,
        String token
) {
}
