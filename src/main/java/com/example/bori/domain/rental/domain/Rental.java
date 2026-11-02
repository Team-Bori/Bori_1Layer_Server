package com.example.bori.domain.rental.domain;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 대여는 신청 즉시 확정, 기간 무제한. 반납은 학생 신청 → 관리자가 실물 확인 후 승인.
@Getter
@Entity
@Table(name = "rental")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_board_id")
    private RentalBoard rentalBoard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentalStatus status;

    @Column(name = "rented_at", nullable = false)
    private LocalDateTime rentedAt;

    @Column(name = "return_requested_at")
    private LocalDateTime returnRequestedAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    public Rental(RentalBoard rentalBoard, User user, LocalDateTime now) {
        this.rentalBoard = rentalBoard;
        this.user = user;
        this.status = RentalStatus.RENTED;
        this.rentedAt = now;
    }

    public void requestReturn(LocalDateTime now) {
        this.status = RentalStatus.RETURN_REQUESTED;
        this.returnRequestedAt = now;
    }

    public void approveReturn(User admin, LocalDateTime now) {
        this.status = RentalStatus.RETURNED;
        this.returnedAt = now;
        this.approvedBy = admin;
    }
}
