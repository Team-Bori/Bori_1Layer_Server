package com.example.bori.domain.queue.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    @EntityGraph(attributePaths = "cloudBoard")
    Optional<QueueEntry> findByUserIdAndStateIn(Long userId, Collection<QueueState> states);

    // 활성 대기 (등록 순)
    List<QueueEntry> findAllByCloudBoardIdAndStateInOrderByPositionAsc(Long boardId, Collection<QueueState> states);

    List<QueueEntry> findAllByStateIn(Collection<QueueState> states);

    @Query("select coalesce(max(q.position), 0) from QueueEntry q where q.cloudBoard.id = :boardId")
    int findMaxPosition(@Param("boardId") Long boardId);

    @Query("select q.id from QueueEntry q where q.state = com.example.bori.domain.queue.domain.QueueState.READY and q.expiresAt < :now")
    List<Long> findExpiredReadyIds(@Param("now") LocalDateTime now);

    // 대기자는 있는데 차례(READY)인 사람이 없는 보드
    @Query("""
            select distinct q.cloudBoard.id from QueueEntry q
            where q.state = com.example.bori.domain.queue.domain.QueueState.WAITING
              and not exists (select 1 from QueueEntry r where r.cloudBoard = q.cloudBoard
                              and r.state = com.example.bori.domain.queue.domain.QueueState.READY)
            """)
    List<Long> findBoardIdsWithWaitingButNoReady();
}
