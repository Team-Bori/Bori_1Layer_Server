package com.example.bori.domain.board.domain;

import com.example.bori.domain.model.domain.AiModel;
import com.example.bori.domain.pi.domain.HostPi;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 삭제는 deleted_at만 채운다(소프트 삭제). 지난 세션 이력이 이 행을 참조한다.
@Getter
@Entity
@Table(name = "cloud_board")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CloudBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pi_id")
    private HostPi pi;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String spec;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false, length = 20)
    private BoardType boardType;

    @Column(nullable = false)
    private String host;

    @Column(name = "upload_limit_mb", nullable = false)
    private int uploadLimitMb;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoardStatus status;

    @Column(name = "serial_code", nullable = false, length = 128)
    private String serialCode;

    @Column(name = "usb_slot", nullable = false, length = 32)
    private String usbSlot;

    // L2가 hello/board_attached로 보고하면 true
    @Column(name = "is_online", nullable = false)
    private boolean online;

    @Column(name = "execute_able", nullable = false)
    private boolean executeAble;

    @ManyToMany
    @JoinTable(name = "board_model",
            joinColumns = @JoinColumn(name = "cloud_board_id"),
            inverseJoinColumns = @JoinColumn(name = "ai_model_id"))
    private Set<AiModel> models = new LinkedHashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public CloudBoard(HostPi pi, String name, String spec, BoardType boardType, String host, int uploadLimitMb,
                      String serialCode, String usbSlot, boolean executeAble, Collection<AiModel> models) {
        this.pi = pi;
        this.name = name;
        this.spec = spec;
        this.boardType = boardType;
        this.host = host;
        this.uploadLimitMb = uploadLimitMb;
        this.serialCode = serialCode;
        this.usbSlot = usbSlot;
        this.executeAble = executeAble;
        this.status = BoardStatus.NORMAL;
        this.online = false;
        this.models.addAll(models);
    }

    public void update(HostPi pi, String name, String spec, BoardType boardType, String host, int uploadLimitMb,
                       String serialCode, String usbSlot, boolean executeAble, Collection<AiModel> models) {
        this.pi = pi;
        this.name = name;
        this.spec = spec;
        this.boardType = boardType;
        this.host = host;
        this.uploadLimitMb = uploadLimitMb;
        this.serialCode = serialCode;
        this.usbSlot = usbSlot;
        this.executeAble = executeAble;
        this.models.clear();
        this.models.addAll(models);
    }

    public void changeStatus(BoardStatus status) {
        this.status = status;
    }

    public void changeUploadLimit(int uploadLimitMb) {
        this.uploadLimitMb = uploadLimitMb;
    }

    // L2가 보고한 위치로 옮긴다 (다른 파이나 슬롯으로 옮겨 꽂은 경우 포함)
    public void attach(HostPi pi, String usbSlot) {
        this.pi = pi;
        if (usbSlot != null && !usbSlot.isBlank()) {
            this.usbSlot = usbSlot;
        }
        this.online = true;
    }

    public void markOffline() {
        this.online = false;
    }

    public void delete(LocalDateTime at) {
        this.deletedAt = at;
        this.online = false;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean supports(Long aiModelId) {
        return models.stream().anyMatch(m -> m.getId().equals(aiModelId));
    }

    // 학생이 새로 접속할 수 있는 상태인지 (사용중 여부는 따로 본다)
    public boolean isAccessible() {
        return !isDeleted() && online && executeAble && status == BoardStatus.NORMAL;
    }
}
