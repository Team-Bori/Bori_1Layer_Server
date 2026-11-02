package com.example.bori.domain.stat.service;

import com.example.bori.domain.stat.domain.BoardModelStat;
import com.example.bori.domain.stat.domain.BoardModelStatRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardModelStatService {

    private final BoardModelStatRepository statRepository;

    @Transactional
    public void addRun(Long boardId, Long modelId, double avgFps, Double avgAccuracy) {
        LocalDateTime now = LocalDateTime.now();
        BoardModelStat stat = statRepository.findById(keyOf(boardId, modelId))
                .orElseGet(() -> statRepository.save(new BoardModelStat(boardId, modelId, now)));
        stat.addRun(avgFps, avgAccuracy, now);
    }

    private BoardModelStat.Key keyOf(Long boardId, Long modelId) {
        return BoardModelStat.Key.of(boardId, modelId);
    }
}
