package com.example.bori.domain.metric.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

// 그래프용. [from, to)를 buckets.size()칸으로 나눈 칸별 평균 (샘플 없는 칸·측정 안 하는 항목은 null).
// summary는 칸 평균이 아니라 원래 샘플 기준 최소·평균·최대.
public record MetricSeriesResponse(
        Long sessionId,
        boolean active,
        LocalDateTime from,
        LocalDateTime to,
        double bucketSec,
        long sampleCount,
        List<Bucket> buckets,
        Summary summary,
        Bucket latest
) {

    public record Bucket(LocalDateTime t, Double fps, Double memPct, Double cpuPct, Double tempC, Double powerW,
                         Double latencyMs, Double accuracyPct) {
    }

    public record Stat(Double min, Double avg, Double max) {
    }

    public record Summary(Stat fps, Stat memPct, Stat cpuPct, Stat tempC, Stat powerW, Stat latencyMs, Stat accuracyPct) {
    }
}
