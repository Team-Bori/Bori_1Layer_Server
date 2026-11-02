package com.example.bori.domain.rental.service;

import com.example.bori.domain.adminlog.domain.AdminAction;
import com.example.bori.domain.adminlog.domain.TargetType;
import com.example.bori.domain.adminlog.service.AdminActionLogService;
import com.example.bori.domain.notification.domain.NotificationType;
import com.example.bori.domain.notification.service.NotificationService;
import com.example.bori.domain.rental.controller.dto.AdminRentalBoardResponse;
import com.example.bori.domain.rental.controller.dto.AdminRentalResponse;
import com.example.bori.domain.rental.controller.dto.RentalBoardRequest;
import com.example.bori.domain.rental.domain.Rental;
import com.example.bori.domain.rental.domain.RentalBoard;
import com.example.bori.domain.rental.domain.RentalBoardRepository;
import com.example.bori.domain.rental.domain.RentalBoardStatus;
import com.example.bori.domain.rental.domain.RentalRepository;
import com.example.bori.domain.rental.domain.RentalStatus;
import com.example.bori.domain.user.domain.User;
import com.example.bori.domain.user.domain.UserRepository;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import com.example.bori.global.storage.FileStorage;
import com.example.bori.global.tx.AfterCommit;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRentalService {

    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private final RentalBoardRepository rentalBoardRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AdminActionLogService adminActionLogService;
    private final FileStorage fileStorage;

    public List<AdminRentalBoardResponse> getBoards() {
        Map<Long, Rental> active = rentalRepository.findAllByStatusInOrderByIdAsc(RentalStatus.ACTIVE).stream()
                .collect(Collectors.toMap(r -> r.getRentalBoard().getId(), Function.identity(), (a, b) -> b));
        return rentalBoardRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .map(b -> AdminRentalBoardResponse.of(b, active.get(b.getId())))
                .toList();
    }

    @Transactional
    public AdminRentalBoardResponse create(Long adminId, RentalBoardRequest request) {
        RentalBoard board = rentalBoardRepository.save(
                new RentalBoard(request.name().trim(), request.spec(), request.accessories()));
        adminActionLogService.record(adminId, AdminAction.CREATE, TargetType.RENTAL_BOARD, board.getId(), board.getName() + " 등록");
        return AdminRentalBoardResponse.of(board, null);
    }

    @Transactional
    public AdminRentalBoardResponse update(Long adminId, Long boardId, RentalBoardRequest request) {
        RentalBoard board = getBoard(boardId);
        board.update(request.name().trim(), request.spec(), request.accessories());
        adminActionLogService.record(adminId, AdminAction.UPDATE, TargetType.RENTAL_BOARD, board.getId(), board.getName() + " 정보 수정");
        return AdminRentalBoardResponse.of(board, activeRental(boardId));
    }

    // 사진은 S3에 올리고 URL만 저장한다. 이전 사진은 커밋 뒤 지운다.
    @Transactional
    public AdminRentalBoardResponse uploadPhoto(Long adminId, Long boardId, MultipartFile file) {
        RentalBoard board = getBoard(boardId);
        String ext = file == null || file.isEmpty() ? null : IMAGE_TYPES.get(file.getContentType());
        if (ext == null) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        String key = "rental-boards/%d/%s.%s".formatted(boardId, UUID.randomUUID(), ext);
        String url;
        try (InputStream in = file.getInputStream()) {
            url = fileStorage.upload(key, in, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        String previous = board.getPhotoUrl();
        board.changePhoto(url);
        AfterCommit.run(() -> fileStorage.deleteByUrl(previous));
        adminActionLogService.record(adminId, AdminAction.UPDATE, TargetType.RENTAL_BOARD, board.getId(), board.getName() + " 사진 변경");
        return AdminRentalBoardResponse.of(board, activeRental(boardId));
    }

    // 관리자가 고를 수 있는 건 대여가능·점검중·고장뿐이고, 대여 중인 보드는 바꿀 수 없다.
    @Transactional
    public AdminRentalBoardResponse changeStatus(Long adminId, Long boardId, RentalBoardStatus status) {
        RentalBoard board = rentalBoardRepository.findForUpdate(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RENTAL_BOARD_NOT_FOUND));
        if (!status.isManual()) {
            throw new BusinessException(ErrorCode.RENTAL_STATUS_NOT_MANUAL);
        }
        if (activeRental(boardId) != null) {
            throw new BusinessException(ErrorCode.RENTAL_BOARD_IN_USE);
        }
        RentalBoardStatus before = board.getStatus();
        if (before != status) {
            board.changeStatus(status);
            adminActionLogService.record(adminId, AdminAction.STATUS_CHANGE, TargetType.RENTAL_BOARD, board.getId(),
                    "%s: %s → %s".formatted(board.getName(), before.getLabel(), status.getLabel()));
        }
        return AdminRentalBoardResponse.of(board, null);
    }

    // 대여 중이어도 지울 수 있다 (디자인: "그래도 삭제할까요?"). 진행 중 대여는 남아서 반납 승인할 수 있다.
    @Transactional
    public void delete(Long adminId, Long boardId) {
        RentalBoard board = getBoard(boardId);
        board.delete(LocalDateTime.now());
        adminActionLogService.record(adminId, AdminAction.DELETE, TargetType.RENTAL_BOARD, board.getId(), board.getName() + " 삭제");
    }

    public List<AdminRentalResponse> getReturnRequests() {
        return rentalRepository.findAllByStatusInOrderByIdAsc(List.of(RentalStatus.RETURN_REQUESTED)).stream()
                .map(AdminRentalResponse::from)
                .toList();
    }

    // 실물을 확인하고 승인 → 대여가능으로 돌리고 학생에게 알린다.
    @Transactional
    public AdminRentalResponse approveReturn(Long adminId, Long rentalId) {
        Rental rental = rentalRepository.findWithBoardAndUserById(rentalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RENTAL_NOT_FOUND));
        if (rental.getStatus() != RentalStatus.RETURN_REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_RENTAL_STATE);
        }
        RentalBoard board = rental.getRentalBoard();
        User student = rental.getUser();
        rental.approveReturn(userRepository.getReferenceById(adminId), LocalDateTime.now());
        if (!board.isDeleted()) {
            board.changeStatus(RentalBoardStatus.AVAILABLE);
        }
        notificationService.notify(student.getId(), NotificationType.RETURN_APPROVED, board.getName() + " 반납이 승인되었습니다.");
        adminActionLogService.record(adminId, AdminAction.RETURN_APPROVE, TargetType.RENTAL, rental.getId(),
                "%s · %s (%s)".formatted(board.getName(), student.getName(), student.getStudentNo()));
        return AdminRentalResponse.from(rental);
    }

    private RentalBoard getBoard(Long boardId) {
        return rentalBoardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RENTAL_BOARD_NOT_FOUND));
    }

    private Rental activeRental(Long boardId) {
        return rentalRepository.findByRentalBoardIdAndStatusIn(boardId, RentalStatus.ACTIVE).orElse(null);
    }
}
