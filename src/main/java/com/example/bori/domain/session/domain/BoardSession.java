package com.example.bori.domain.session.domain;

import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.model.domain.AiModel;
import com.example.bori.domain.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 한 번의 접속. ended_at이 NULL이면 진행 중이다.
// 모델 출처는 검증 모델(ai_model)과 학생 업로드(upload) 중 정확히 하나 (DB CHECK).
@Getter
@Entity
@Table(name = "board_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_board_id")
    private CloudBoard cloudBoard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_model_id")
    private AiModel aiModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id")
    private UserModelUpload upload;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    // 마지막 조작 시각. 15분 유휴 판정 기준
    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 20)
    private EndReason endReason;

    private BoardSession(User user, CloudBoard cloudBoard, AiModel aiModel, UserModelUpload upload, LocalDateTime now) {
        this.user = user;
        this.cloudBoard = cloudBoard;
        this.aiModel = aiModel;
        this.upload = upload;
        this.startedAt = now;
        this.lastActiveAt = now;
    }

    public static BoardSession withVerifiedModel(User user, CloudBoard board, AiModel model, LocalDateTime now) {
        return new BoardSession(user, board, model, null, now);
    }

    public static BoardSession withUpload(User user, CloudBoard board, UserModelUpload upload, LocalDateTime now) {
        return new BoardSession(user, board, null, upload, now);
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public boolean isVerifiedModel() {
        return aiModel != null;
    }

    public void touch(LocalDateTime now) {
        if (isActive()) {
            this.lastActiveAt = now;
        }
    }

    // 이미 끝났으면 false
    public boolean end(LocalDateTime now, EndReason reason) {
        if (!isActive()) {
            return false;
        }
        this.endedAt = now;
        this.endReason = reason;
        return true;
    }

    public String modelLabel() {
        return aiModel != null ? aiModel.getName() : "사용자 모델 · " + upload.repoPath();
    }

    public long durationSec(LocalDateTime now) {
        return Duration.between(startedAt, endedAt != null ? endedAt : now).toSeconds();
    }
}
