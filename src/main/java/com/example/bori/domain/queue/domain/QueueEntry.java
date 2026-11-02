package com.example.bori.domain.queue.domain;

import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// position은 보드별 등록 순서(계속 증가). 화면의 "순번"은 활성 대기 중 몇 번째인지로 계산한다.
@Getter
@Entity
@Table(name = "queue_entry")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QueueEntry {

    public static final Duration READY_TIMEOUT = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_board_id")
    private CloudBoard cloudBoard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueState state;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public QueueEntry(CloudBoard cloudBoard, User user, int position) {
        this.cloudBoard = cloudBoard;
        this.user = user;
        this.position = position;
        this.state = QueueState.WAITING;
    }

    public boolean isActive() {
        return QueueState.ACTIVE.contains(state);
    }

    public void ready(LocalDateTime now) {
        this.state = QueueState.READY;
        this.notifiedAt = now;
        this.expiresAt = now.plus(READY_TIMEOUT);
    }

    public void enter() {
        this.state = QueueState.ENTERED;
    }

    public void expire() {
        this.state = QueueState.EXPIRED;
    }

    public void cancel() {
        this.state = QueueState.CANCELLED;
    }
}
