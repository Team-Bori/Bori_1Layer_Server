package com.example.bori.domain.metric.service;

import com.example.bori.domain.metric.controller.dto.MetricSeriesResponse;
import com.example.bori.domain.metric.controller.dto.MetricSeriesResponse.Bucket;
import com.example.bori.domain.metric.controller.dto.MetricSeriesResponse.Stat;
import com.example.bori.domain.metric.controller.dto.MetricSeriesResponse.Summary;
import com.example.bori.domain.session.domain.BoardSession;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricQueryService {

    // 화면의 범위 선택: 5분 ~ 12시간
    public static final Set<Integer> RANGES = Set.of(300, 900, 1800, 3600, 10800, 21600, 43200);
    private static final int BUCKETS = 60;
    private static final String[] COLUMNS = {"fps", "mem_pct", "cpu_pct", "temp_c", "power_w", "latency_ms", "accuracy_pct"};

    private final BoardSessionRepository boardSessionRepository;
    private final NamedParameterJdbcTemplate jdbc;

    // 진행 중 세션: 최근 rangeSec초. 끝난 세션: 시작~종료 전체 (rangeSec 무시).
    public MetricSeriesResponse getSeries(Long userId, boolean admin, Long sessionId, Integer rangeSec) {
        BoardSession session = boardSessionRepository.findById(sessionId)
                .filter(s -> admin || s.getUser().getId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        int range = rangeSec == null ? 300 : rangeSec;
        if (!RANGES.contains(range)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        LocalDateTime to = session.isActive() ? LocalDateTime.now() : session.getEndedAt();
        LocalDateTime from = session.isActive() ? to.minusSeconds(range) : session.getStartedAt();
        double bucketSec = Math.max(1, Duration.between(from, to).toMillis() / 1000.0 / BUCKETS);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("sid", sessionId)
                .addValue("from", Timestamp.valueOf(from))
                .addValue("to", Timestamp.valueOf(to))
                .addValue("bucketSec", bucketSec);

        Bucket[] buckets = new Bucket[BUCKETS];
        for (int i = 0; i < BUCKETS; i++) {
            buckets[i] = new Bucket(from.plusNanos((long) (bucketSec * i * 1e9)), null, null, null, null, null, null, null);
        }
        jdbc.query("""
                select least(floor(extract(epoch from (sampled_at - cast(:from as timestamp))) / :bucketSec)::int, %d) as b,
                       avg(fps) fps, avg(mem_pct) mem_pct, avg(cpu_pct) cpu_pct, avg(temp_c) temp_c,
                       avg(power_w) power_w, avg(latency_ms) latency_ms, avg(accuracy_pct) accuracy_pct
                from metric_sample
                where session_id = :sid and sampled_at >= cast(:from as timestamp) and sampled_at < cast(:to as timestamp)
                group by b
                """.formatted(BUCKETS - 1), params, rs -> {
            int b = rs.getInt("b");
            buckets[b] = new Bucket(buckets[b].t(), num(rs, "fps"), num(rs, "mem_pct"), num(rs, "cpu_pct"),
                    num(rs, "temp_c"), num(rs, "power_w"), num(rs, "latency_ms"), num(rs, "accuracy_pct"));
        });

        StringBuilder statSql = new StringBuilder("select count(*) n");
        for (String c : COLUMNS) {
            statSql.append(", min(%1$s) %1$s_min, avg(%1$s) %1$s_avg, max(%1$s) %1$s_max".formatted(c));
        }
        statSql.append(" from metric_sample where session_id = :sid"
                + " and sampled_at >= cast(:from as timestamp) and sampled_at < cast(:to as timestamp)");
        Object[] summaryHolder = new Object[2];
        jdbc.query(statSql.toString(), params, rs -> {
            summaryHolder[0] = rs.getLong("n");
            summaryHolder[1] = new Summary(stat(rs, "fps"), stat(rs, "mem_pct"), stat(rs, "cpu_pct"), stat(rs, "temp_c"),
                    stat(rs, "power_w"), stat(rs, "latency_ms"), stat(rs, "accuracy_pct"));
        });

        List<Bucket> latest = jdbc.query("""
                select sampled_at, fps, mem_pct, cpu_pct, temp_c, power_w, latency_ms, accuracy_pct
                from metric_sample where session_id = :sid order by sampled_at desc limit 1
                """, params, (rs, i) -> new Bucket(rs.getTimestamp("sampled_at").toLocalDateTime(), num(rs, "fps"),
                num(rs, "mem_pct"), num(rs, "cpu_pct"), num(rs, "temp_c"), num(rs, "power_w"), num(rs, "latency_ms"),
                num(rs, "accuracy_pct")));

        return new MetricSeriesResponse(sessionId, session.isActive(), from, to, bucketSec, (long) summaryHolder[0],
                new ArrayList<>(Arrays.asList(buckets)), (Summary) summaryHolder[1], latest.isEmpty() ? null : latest.getFirst());
    }

    private static Stat stat(ResultSet rs, String column) throws SQLException {
        return new Stat(num(rs, column + "_min"), num(rs, column + "_avg"), num(rs, column + "_max"));
    }

    private static Double num(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
