package com.example.bori.domain.session.domain;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardSessionRepository extends JpaRepository<BoardSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BoardSession s where s.id = :id")
    Optional<BoardSession> findForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"cloudBoard", "cloudBoard.pi", "aiModel", "upload"})
    Optional<BoardSession> findByUserIdAndEndedAtIsNull(Long userId);

    Optional<BoardSession> findByCloudBoardIdAndEndedAtIsNull(Long boardId);

    boolean existsByCloudBoardIdAndEndedAtIsNull(Long boardId);

    @EntityGraph(attributePaths = {"user", "cloudBoard", "cloudBoard.pi", "aiModel", "upload"})
    List<BoardSession> findAllByEndedAtIsNull();

    @Query("select s from BoardSession s join fetch s.cloudBoard b where b.pi.id = :piId and s.endedAt is null")
    List<BoardSession> findActiveByPiId(@Param("piId") Long piId);

    @Query("select s.id from BoardSession s where s.endedAt is null and s.lastActiveAt < :cutoff")
    List<Long> findIdleIds(@Param("cutoff") LocalDateTime cutoff);

    @EntityGraph(attributePaths = {"cloudBoard", "aiModel", "upload"})
    List<BoardSession> findAllByUserIdAndStartedAtAfterOrderByIdDesc(Long userId, LocalDateTime after);

    // 관리자 접속 이력: 사용자 이름·학번·보드명으로 검색. q가 빈 문자열이면 전체.
    @EntityGraph(attributePaths = {"user", "cloudBoard", "aiModel", "upload"})
    @Query("""
            select s from BoardSession s
            where lower(s.user.name) like lower(concat('%', :q, '%'))
               or coalesce(s.user.studentNo, '') like concat('%', :q, '%')
               or lower(s.cloudBoard.name) like lower(concat('%', :q, '%'))
            order by s.id desc
            """)
    Page<BoardSession> search(@Param("q") String q, Pageable pageable);

    @Query("select s from BoardSession s join fetch s.cloudBoard b join fetch b.pi where s.id = :id")
    Optional<BoardSession> findWithBoard(@Param("id") Long id);
}
