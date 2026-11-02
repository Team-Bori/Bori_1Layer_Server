package com.example.bori.domain.session.domain;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserModelUploadRepository extends JpaRepository<UserModelUpload, Long> {

    @EntityGraph(attributePaths = "cloudBoard")
    List<UserModelUpload> findAllByUserIdOrderByIdDesc(Long userId);
}
