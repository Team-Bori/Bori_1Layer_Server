package com.example.bori.domain.rental.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    @EntityGraph(attributePaths = "rentalBoard")
    List<Rental> findAllByUserIdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = {"rentalBoard", "user"})
    List<Rental> findAllByStatusInOrderByIdAsc(Collection<RentalStatus> statuses);

    @EntityGraph(attributePaths = {"rentalBoard", "user"})
    Optional<Rental> findWithBoardAndUserById(Long id);

    Optional<Rental> findByRentalBoardIdAndStatusIn(Long boardId, Collection<RentalStatus> statuses);

    // 이력 조회: 보드명·사용자 이름·학번으로 검색. q가 빈 문자열이면 전체.
    @EntityGraph(attributePaths = {"rentalBoard", "user"})
    @Query("""
            select r from Rental r
            where lower(r.rentalBoard.name) like lower(concat('%', :q, '%'))
               or lower(r.user.name) like lower(concat('%', :q, '%'))
               or coalesce(r.user.studentNo, '') like concat('%', :q, '%')
            order by r.id desc
            """)
    Page<Rental> search(@Param("q") String q, Pageable pageable);
}
