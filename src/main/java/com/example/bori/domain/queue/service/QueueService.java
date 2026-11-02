package com.example.bori.domain.queue.service;

import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.board.domain.CloudBoardRepository;
import com.example.bori.domain.notification.domain.NotificationType;
import com.example.bori.domain.notification.service.NotificationService;
import com.example.bori.domain.queue.controller.dto.MyQueueResponse;
import com.example.bori.domain.queue.domain.QueueEntry;
import com.example.bori.domain.queue.domain.QueueEntryRepository;
import com.example.bori.domain.queue.domain.QueueState;
import com.example.bori.domain.queue.event.BoardFreedEvent;
import com.example.bori.domain.session.domain.BoardSessionRepository;
import com.example.bori.domain.user.domain.UserRepository;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import com.example.bori.global.push.ClientPushService;
import com.example.bori.global.push.PushType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 보드당 대기 2명. 사용 중인 보드에만 등록할 수 있고, 사용자당 활성 대기는 1건.
// 보드가 비면 첫 대기자를 READY로 올리고 알린다. READY는 5분 안에 접속해야 한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueService {

    public static final int CAPACITY = 2;
    // 대기가 없어졌을 때 브라우저로 보내는 값
    private static final Object NO_QUEUE = null;

    private final QueueEntryRepository queueEntryRepository;
    private final CloudBoardRepository cloudBoardRepository;
    private final BoardSessionRepository boardSessionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ClientPushService clientPushService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MyQueueResponse join(Long userId, Long boardId) {
        CloudBoard board = cloudBoardRepository.findForUpdate(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));
        if (boardSessionRepository.findByUserIdAndEndedAtIsNull(userId).isPresent()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ACTIVE);
        }
        if (queueEntryRepository.findByUserIdAndStateIn(userId, QueueState.ACTIVE).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_QUEUED);
        }
        if (!board.isAccessible()) {
            throw new BusinessException(ErrorCode.BOARD_UNAVAILABLE);
        }
        List<QueueEntry> active = activeEntries(boardId);
        boolean busy = boardSessionRepository.existsByCloudBoardIdAndEndedAtIsNull(boardId);
        if (!busy && active.isEmpty()) {
            throw new BusinessException(ErrorCode.QUEUE_NOT_NEEDED);
        }
        if (active.size() >= CAPACITY) {
            throw new BusinessException(ErrorCode.QUEUE_FULL);
        }
        int position = queueEntryRepository.findMaxPosition(boardId) + 1;
        QueueEntry entry = queueEntryRepository.save(new QueueEntry(board, userRepository.getReferenceById(userId), position));
        return toResponse(entry, board, active.size() + 1);
    }

    @Transactional
    public void leave(Long userId) {
        QueueEntry entry = queueEntryRepository.findByUserIdAndStateIn(userId, QueueState.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUEUE_NOT_FOUND));
        entry.cancel();
        clientPushService.push(userId, PushType.QUEUE, NO_QUEUE);
        promoteAfterCommit(entry.getCloudBoard().getId());
    }

    public Optional<MyQueueResponse> getMyQueue(Long userId) {
        return queueEntryRepository.findByUserIdAndStateIn(userId, QueueState.ACTIVE)
                .map(entry -> {
                    List<QueueEntry> active = activeEntries(entry.getCloudBoard().getId());
                    return toResponse(entry, entry.getCloudBoard(), active.indexOf(entry) + 1);
                });
    }

    // 보드가 비었을 때 첫 대기자를 READY로 올린다. 이미 READY가 있거나 보드를 쓸 수 없으면 그대로 둔다.
    // 대기가 빠진 뒤에도 불려서 순번이 바뀐 대기자에게 현재 상태를 보낸다.
    @Transactional
    public void promoteNext(Long boardId) {
        promote(boardId, true);
    }

    // 주기 점검용. 실제로 차례를 넘겼을 때만 알린다.
    @Transactional
    public void promoteIfIdle(Long boardId) {
        promote(boardId, false);
    }

    private void promote(Long boardId, boolean alwaysBroadcast) {
        Optional<CloudBoard> found = cloudBoardRepository.findForUpdate(boardId);
        if (found.isEmpty()) {
            return;
        }
        CloudBoard board = found.get();
        List<QueueEntry> active = activeEntries(boardId);
        boolean canPromote = board.isAccessible()
                && !boardSessionRepository.existsByCloudBoardIdAndEndedAtIsNull(boardId)
                && active.stream().noneMatch(e -> e.getState() == QueueState.READY);
        Optional<QueueEntry> next = canPromote
                ? active.stream().filter(e -> e.getState() == QueueState.WAITING).findFirst()
                : Optional.empty();
        next.ifPresent(entry -> {
            entry.ready(LocalDateTime.now());
            notificationService.notify(entry.getUser().getId(), NotificationType.QUEUE_READY,
                    board.getName() + " 차례가 되었습니다. 5분 안에 접속하세요.");
        });
        if (alwaysBroadcast || next.isPresent()) {
            broadcast(board, active);
        }
    }

    // READY 5분이 지난 대기를 만료시킨다.
    @Transactional
    public void expire(Long entryId) {
        queueEntryRepository.findById(entryId)
                .filter(e -> e.getState() == QueueState.READY && e.getExpiresAt().isBefore(LocalDateTime.now()))
                .ifPresent(entry -> {
                    entry.expire();
                    clientPushService.push(entry.getUser().getId(), PushType.QUEUE,
                            toResponse(entry, entry.getCloudBoard(), 0));
                    promoteAfterCommit(entry.getCloudBoard().getId());
                });
    }

    // 보드를 지울 때 대기를 모두 취소한다.
    @Transactional
    public void cancelAll(Long boardId) {
        for (QueueEntry entry : activeEntries(boardId)) {
            entry.cancel();
            clientPushService.push(entry.getUser().getId(), PushType.QUEUE, NO_QUEUE);
        }
    }

    // 접속에 쓰인 READY 대기를 ENTERED로. 세션 시작과 같은 트랜잭션에서 부른다.
    @Transactional
    public void enter(QueueEntry entry) {
        entry.enter();
        clientPushService.push(entry.getUser().getId(), PushType.QUEUE, NO_QUEUE);
        promoteAfterCommit(entry.getCloudBoard().getId());
    }

    public List<QueueEntry> activeEntries(Long boardId) {
        return queueEntryRepository.findAllByCloudBoardIdAndStateInOrderByPositionAsc(boardId, QueueState.ACTIVE);
    }

    public void promoteAfterCommit(Long boardId) {
        eventPublisher.publishEvent(new BoardFreedEvent(boardId));
    }

    // 순번이 바뀌었을 수 있으니 이 보드 대기자 모두에게 현재 상태를 보낸다.
    private void broadcast(CloudBoard board, List<QueueEntry> active) {
        for (int i = 0; i < active.size(); i++) {
            QueueEntry entry = active.get(i);
            if (entry.isActive()) {
                clientPushService.push(entry.getUser().getId(), PushType.QUEUE, toResponse(entry, board, i + 1));
            }
        }
    }

    private MyQueueResponse toResponse(QueueEntry entry, CloudBoard board, int position) {
        return new MyQueueResponse(entry.getId(), board.getId(), board.getName(), entry.getState(), position, CAPACITY,
                entry.getExpiresAt());
    }
}
