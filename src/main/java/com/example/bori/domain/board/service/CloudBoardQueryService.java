package com.example.bori.domain.board.service;

import com.example.bori.domain.board.controller.dto.CloudBoardResponse;
import com.example.bori.domain.board.domain.BoardStatus;
import com.example.bori.domain.board.domain.BoardType;
import com.example.bori.domain.board.domain.CloudAvailability;
import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.board.domain.CloudBoardRepository;
import com.example.bori.domain.queue.domain.QueueEntry;
import com.example.bori.domain.queue.domain.QueueEntryRepository;
import com.example.bori.domain.queue.domain.QueueState;
import com.example.bori.domain.queue.service.QueueService;
import com.example.bori.domain.session.domain.BoardSession;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.domain.stat.domain.BoardModelStat;
import com.example.bori.domain.stat.domain.BoardModelStatRepository;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CloudBoardQueryService {

    private final CloudBoardRepository cloudBoardRepository;
    private final BoardSessionRepository boardSessionRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final BoardModelStatRepository statRepository;

    public List<CloudBoardResponse> getBoards(Long userId, BoardType type, String q) {
        String keyword = q == null ? "" : q.trim().toLowerCase();
        List<CloudBoard> boards = cloudBoardRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .filter(b -> type == null || b.getBoardType() == type)
                .filter(b -> keyword.isEmpty() || b.getName().toLowerCase().contains(keyword))
                .toList();
        return toResponses(userId, boards);
    }

    public CloudBoardResponse getBoard(Long userId, Long boardId) {
        CloudBoard board = cloudBoardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));
        return toResponses(userId, List.of(board)).getFirst();
    }

    private List<CloudBoardResponse> toResponses(Long userId, List<CloudBoard> boards) {
        Map<Long, BoardSession> activeSessions = boardSessionRepository.findAllByEndedAtIsNull().stream()
                .collect(Collectors.toMap(s -> s.getCloudBoard().getId(), Function.identity()));
        Map<Long, List<QueueEntry>> queues = queueEntryRepository.findAllByStateIn(QueueState.ACTIVE).stream()
                .collect(Collectors.groupingBy(e -> e.getCloudBoard().getId()));
        Map<Long, List<BoardModelStat>> stats = statRepository.findAllByCloudBoardIdIn(
                        boards.stream().map(CloudBoard::getId).toList()).stream()
                .collect(Collectors.groupingBy(BoardModelStat::getCloudBoardId));

        return boards.stream().map(board -> {
            List<QueueEntry> queue = queues.getOrDefault(board.getId(), List.of());
            Map<Long, BoardModelStat> boardStats = stats.getOrDefault(board.getId(), List.of()).stream()
                    .collect(Collectors.toMap(BoardModelStat::getAiModelId, Function.identity()));
            List<CloudBoardResponse.ModelStat> models = board.getModels().stream()
                    .sorted(Comparator.comparing(m -> m.getId()))
                    .map(m -> {
                        BoardModelStat st = boardStats.get(m.getId());
                        return new CloudBoardResponse.ModelStat(m.getId(), m.getName(), m.getFormat(), m.getDescription(),
                                st == null ? 0 : st.getAvgFps(), st == null ? 0 : st.getAvgAccuracy(),
                                st == null ? 0 : st.getRunCount());
                    })
                    .toList();
            int totalRuns = models.stream().mapToInt(CloudBoardResponse.ModelStat::runCount).sum();
            return new CloudBoardResponse(board.getId(), board.getName(), board.getHost(), board.getBoardType(),
                    board.getSpec(), board.getPi().getName(), board.getUsbSlot(), board.getUploadLimitMb(),
                    board.isOnline(), availability(userId, board, activeSessions.get(board.getId()), queue),
                    queue.size(), QueueService.CAPACITY, totalRuns, models);
        }).toList();
    }

    private CloudAvailability availability(Long userId, CloudBoard board, BoardSession session, List<QueueEntry> queue) {
        if (session != null && session.getUser().getId().equals(userId)) {
            return CloudAvailability.MY_SESSION;
        }
        if (!board.isOnline()) {
            return CloudAvailability.OFFLINE;
        }
        if (board.getStatus() == BoardStatus.MAINTENANCE) {
            return CloudAvailability.MAINTENANCE;
        }
        if (board.getStatus() == BoardStatus.BROKEN) {
            return CloudAvailability.BROKEN;
        }
        if (!board.isExecuteAble()) {
            return CloudAvailability.NOT_EXECUTABLE;
        }
        boolean myTurn = queue.stream()
                .anyMatch(e -> e.getState() == QueueState.READY && e.getUser().getId().equals(userId));
        if (myTurn) {
            return CloudAvailability.MY_TURN;
        }
        if (session != null) {
            return CloudAvailability.BUSY;
        }
        return queue.isEmpty() ? CloudAvailability.FREE : CloudAvailability.RESERVED;
    }
}
