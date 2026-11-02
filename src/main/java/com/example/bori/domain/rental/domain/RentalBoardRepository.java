package com.example.bori.domain.rental.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalBoardRepository extends JpaRepository<RentalBoard, Long> {

    List<RentalBoard> findAllByDeletedAtIsNullOrderByIdAsc();

    Optional<RentalBoard> findByIdAndDeletedAtIsNull(Long id);

    // 같은 보드를 두 명이 동시에 빌리지 못하게
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from RentalBoard b where b.id = :id and b.deletedAt is null")
    Optional<RentalBoard> findForUpdate(@Param("id") Long id);
}
