package com.example.bori.domain.metric.service;

import com.example.bori.domain.metric.relay.MetricPayloads.Sample;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// 1초에 세션당 1행이라 JPA(IDENTITY라 배치가 안 됨) 대신 JDBC 배치로 넣는다.
@Component
@RequiredArgsConstructor
public class MetricWriter {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final String INSERT = """
            insert into metric_sample (session_id, sampled_at, cpu_pct, mem_pct, fps, temp_c, power_w, latency_ms, accuracy_pct)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public void write(Long sessionId, List<Sample> samples) {
        jdbcTemplate.batchUpdate(INSERT, samples, samples.size(), (ps, s) -> {
            Instant t = s.t() != null ? s.t() : Instant.now();
            ps.setLong(1, sessionId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.ofInstant(t, ZONE)));
            setNumber(ps, 3, s.cpuPct(), 999.99);
            setNumber(ps, 4, s.memPct(), 999.99);
            setNumber(ps, 5, s.fps(), 9999.99);
            setNumber(ps, 6, s.tempC(), 999.99);
            setNumber(ps, 7, s.powerW(), 9999.99);
            setNumber(ps, 8, s.latencyMs(), 999999.99);
            setNumber(ps, 9, s.accuracyPct(), 999.99);
        });
    }

    // 컬럼 numeric 범위를 넘거나 음수·NaN이면 그 값만 버린다 (한 값 때문에 묶음 전체가 실패하지 않게).
    private void setNumber(java.sql.PreparedStatement ps, int index, Double value, double max) throws java.sql.SQLException {
        if (value == null || value.isNaN() || value < 0 || value > max) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setDouble(index, value);
        }
    }
}
