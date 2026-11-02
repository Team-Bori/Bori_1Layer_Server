package com.example.bori.domain.metric.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 지표는 세션당 초당 1행이라 빨리 쌓인다. 보관 기간(기본 30일)이 지난 샘플을 매일 새벽에 지운다.
// 보드×모델 통계(board_model_stat)는 세션 종료 때 이미 반영돼 있어 영향이 없다.
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricRetentionJob {

    private final JdbcTemplate jdbcTemplate;

    @Value("${bori.metric.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purge() {
        int deleted = jdbcTemplate.update(
                "delete from metric_sample where sampled_at < now() - make_interval(days => ?)", retentionDays);
        log.info("지표 보관 기간({}일) 지난 샘플 {}건 삭제", retentionDays, deleted);
    }
}
