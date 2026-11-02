package com.example.bori.domain.session.runtime;

public enum StepState {
    WAITING,
    STARTED,
    DONE,
    FAILED;

    public static StepState fromWire(String value) {
        return switch (value == null ? "" : value) {
            case "started" -> STARTED;
            case "done" -> DONE;
            case "failed" -> FAILED;
            default -> null;
        };
    }
}
