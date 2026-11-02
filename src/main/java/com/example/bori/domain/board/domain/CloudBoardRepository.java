package com.example.bori.domain.board.domain;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CloudBoardRepository extends JpaRepository<CloudBoard, Long> {

    @EntityGraph(attributePaths = {"pi", "models"})
    List<CloudBoard> findAllByDeletedAtIsNullOrderByIdAsc();

    @EntityGraph(attributePaths = {"pi", "models"})
    Optional<CloudBoard> findByIdAndDeletedAtIsNull(Long id);

    // 같은 보드에 대한 접속·대기 등록을 한 줄로 세운다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CloudBoard b where b.id = :id and b.deletedAt is null")
    Optional<CloudBoard> findForUpdate(@Param("id") Long id);

    Optional<CloudBoard> findBySerialCodeAndDeletedAtIsNull(String serialCode);

    List<CloudBoard> findAllByPiIdAndDeletedAtIsNull(Long piId);

    List<CloudBoard> findAllBySerialCodeInAndDeletedAtIsNull(Collection<String> serialCodes);

    boolean existsBySerialCodeAndDeletedAtIsNull(String serialCode);

    boolean existsBySerialCodeAndDeletedAtIsNullAndIdNot(String serialCode, Long id);

    boolean existsByPiIdAndUsbSlotAndDeletedAtIsNull(Long piId, String usbSlot);

    boolean existsByPiIdAndUsbSlotAndDeletedAtIsNullAndIdNot(Long piId, String usbSlot, Long id);

    @Modifying(clearAutomatically = true)
    @Query("update CloudBoard b set b.online = false where b.online = true")
    int markAllOffline();
}
