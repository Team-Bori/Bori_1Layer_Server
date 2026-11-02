package com.example.bori.domain.adminlog.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// q는 빈 문자열이면 전체. (null 파라미터는 PostgreSQL이 타입을 추론하지 못한다)
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    @EntityGraph(attributePaths = "admin")
    @Query("""
            select l from AdminActionLog l
            where lower(l.admin.name) like lower(concat('%', :q, '%'))
               or lower(l.detail) like lower(concat('%', :q, '%'))
            order by l.id desc
            """)
    Page<AdminActionLog> search(@Param("q") String q, Pageable pageable);
}
