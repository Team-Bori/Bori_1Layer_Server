package com.example.bori.domain.session.runtime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 화면의 배포 4단계. fetch는 L1이 하고 나머지는 L2가 deploy_progress로 알린다.
@Getter
@RequiredArgsConstructor
public enum DeployStep {
    FETCH("fetch"),
    TRANSFER("transfer"),
    INSTALL("install"),
    RUN("run");

    private final String wire;

    public static DeployStep fromWire(String value) {
        for (DeployStep step : values()) {
            if (step.wire.equals(value)) {
                return step;
            }
        }
        return null;
    }
}
