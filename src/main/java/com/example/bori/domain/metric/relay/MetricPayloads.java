package com.example.bori.domain.metric.relay;

import java.time.Instant;
import java.util.List;

public final class MetricPayloads {

    public record Sample(Instant t, Double fps, Double latencyMs, Double accuracyPct, Double memPct, Double cpuPct,
                         Double tempC, Double powerW) {
    }

    public record Metric(Long sessionId, List<Sample> samples) {
    }

    public record SerialLog(Long sessionId, List<String> lines) {
    }

    private MetricPayloads() {
    }
}
