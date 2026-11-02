package com.example.bori.domain.stat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 보드 × 검증 모델 성능 집계. 세션이 끝날 때 L2 요약으로 갱신한다.
@Getter
@Entity
@Table(name = "board_model_stat")
@IdClass(BoardModelStat.Key.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardModelStat {

    @Id
    @Column(name = "cloud_board_id")
    private Long cloudBoardId;

    @Id
    @Column(name = "ai_model_id")
    private Long aiModelId;

    @Column(name = "avg_fps", nullable = false, columnDefinition = "numeric(6,2)")
    private double avgFps;

    @Column(name = "avg_accuracy", nullable = false, columnDefinition = "numeric(5,2)")
    private double avgAccuracy;

    @Column(name = "run_count", nullable = false)
    private int runCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BoardModelStat(Long cloudBoardId, Long aiModelId, LocalDateTime now) {
        this.cloudBoardId = cloudBoardId;
        this.aiModelId = aiModelId;
        this.updatedAt = now;
    }

    // 실행 한 번의 평균을 누적 평균(실행 횟수 가중)에 더한다.
    // 정확도를 못 잰 실행(null)은 정확도 평균을 바꾸지 않는다. 정확도만 따로 센 횟수 컬럼이 없어 근사다.
    public void addRun(double fps, Double accuracy, LocalDateTime now) {
        double n = runCount;
        this.avgFps = round2((avgFps * n + fps) / (n + 1));
        if (accuracy != null) {
            this.avgAccuracy = round2((avgAccuracy * n + accuracy) / (n + 1));
        }
        this.runCount++;
        this.updatedAt = now;
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class Key implements Serializable {
        private Long cloudBoardId;
        private Long aiModelId;

        public static Key of(Long cloudBoardId, Long aiModelId) {
            Key key = new Key();
            key.cloudBoardId = cloudBoardId;
            key.aiModelId = aiModelId;
            return key;
        }
    }
}
