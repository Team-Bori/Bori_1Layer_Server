package com.example.bori.domain.stat.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardModelStatRepository extends JpaRepository<BoardModelStat, BoardModelStat.Key> {

    List<BoardModelStat> findAllByCloudBoardIdIn(Collection<Long> boardIds);
}
