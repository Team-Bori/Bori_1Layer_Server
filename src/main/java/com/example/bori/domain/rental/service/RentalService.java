package com.example.bori.domain.rental.service;

import com.example.bori.domain.rental.controller.dto.MyRentalResponse;
import com.example.bori.domain.rental.controller.dto.RentalBoardResponse;
import com.example.bori.domain.rental.domain.Rental;
import com.example.bori.domain.rental.domain.RentalBoard;
import com.example.bori.domain.rental.domain.RentalBoardRepository;
import com.example.bori.domain.rental.domain.RentalBoardStatus;
import com.example.bori.domain.rental.domain.RentalRepository;
import com.example.bori.domain.rental.domain.RentalStatus;
import com.example.bori.domain.user.domain.UserRepository;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 학생 대여: 신청 즉시 확정, 기간 무제한. 수령·반납은 오프라인.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalService {

    private final RentalBoardRepository rentalBoardRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;

    public List<RentalBoardResponse> getBoards(RentalBoardStatus status) {
        return rentalBoardRepository.findAllByDeletedAtIsNullOrderByIdAsc().stream()
                .filter(b -> status == null || b.getStatus() == status)
                .map(RentalBoardResponse::from)
                .toList();
    }

    @Transactional
    public MyRentalResponse rent(Long userId, Long boardId) {
        RentalBoard board = rentalBoardRepository.findForUpdate(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RENTAL_BOARD_NOT_FOUND));
        if (board.getStatus() != RentalBoardStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.RENTAL_BOARD_NOT_AVAILABLE);
        }
        board.changeStatus(RentalBoardStatus.RENTED);
        Rental rental = rentalRepository.save(new Rental(board, userRepository.getReferenceById(userId), LocalDateTime.now()));
        return MyRentalResponse.from(rental);
    }

    public List<MyRentalResponse> getMyRentals(Long userId) {
        return rentalRepository.findAllByUserIdOrderByIdDesc(userId).stream()
                .map(MyRentalResponse::from)
                .toList();
    }

    @Transactional
    public MyRentalResponse requestReturn(Long userId, Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .filter(r -> r.getUser().getId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RENTAL_NOT_FOUND));
        if (rental.getStatus() != RentalStatus.RENTED) {
            throw new BusinessException(ErrorCode.INVALID_RENTAL_STATE);
        }
        rental.requestReturn(LocalDateTime.now());
        rental.getRentalBoard().changeStatus(RentalBoardStatus.RETURN_PENDING);
        return MyRentalResponse.from(rental);
    }
}
