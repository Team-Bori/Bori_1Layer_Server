package com.example.bori.domain.rental.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 실물 대여용 보드 (전원 안 꽂힌 여유 보드). 삭제는 소프트 삭제 — 대여 이력이 참조한다.
@Getter
@Entity
@Table(name = "rental_board")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RentalBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String spec;

    // S3 객체 URL
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(length = 500)
    private String accessories;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentalBoardStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public RentalBoard(String name, String spec, String accessories) {
        this.name = name;
        this.spec = spec;
        this.accessories = accessories;
        this.status = RentalBoardStatus.AVAILABLE;
    }

    public void update(String name, String spec, String accessories) {
        this.name = name;
        this.spec = spec;
        this.accessories = accessories;
    }

    public void changePhoto(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void changeStatus(RentalBoardStatus status) {
        this.status = status;
    }

    public void delete(LocalDateTime at) {
        this.deletedAt = at;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
