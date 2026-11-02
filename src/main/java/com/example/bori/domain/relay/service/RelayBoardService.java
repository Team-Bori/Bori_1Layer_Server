package com.example.bori.domain.relay.service;

import com.example.bori.domain.board.domain.CloudBoard;
import com.example.bori.domain.board.domain.CloudBoardRepository;
import com.example.bori.domain.pi.domain.HostPi;
import com.example.bori.domain.pi.domain.HostPiRepository;
import com.example.bori.domain.relay.message.RelayPayloads.BoardReport;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// L2 보고로 보드 온라인 여부·위치를 맞춘다. 보드 등록은 관리자만 하고, 미등록 시리얼은 무시한다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RelayBoardService {

    private final HostPiRepository hostPiRepository;
    private final CloudBoardRepository cloudBoardRepository;

    public record HelloResult(boolean piMismatch, List<String> unknownBoards, Map<String, String> reported) {
    }

    public HelloResult handleHello(Long piId, String piSerial, List<BoardReport> boards) {
        Optional<HostPi> found = hostPiRepository.findById(piId);
        if (found.isEmpty() || !found.get().getSerialCode().equals(piSerial)) {
            return new HelloResult(true, List.of(), Map.of());
        }
        HostPi pi = found.get();
        pi.markUsed(LocalDateTime.now());

        Map<String, String> reported = new LinkedHashMap<>();
        if (boards != null) {
            boards.stream()
                    .filter(b -> b != null && b.serialCode() != null && !b.serialCode().isBlank())
                    .forEach(b -> reported.put(b.serialCode(), b.usbSlot()));
        }

        List<CloudBoard> registered = cloudBoardRepository.findAllBySerialCodeInAndDeletedAtIsNull(reported.keySet());
        registered.forEach(board -> board.attach(pi, reported.get(board.getSerialCode())));

        Set<String> registeredSerials = registered.stream().map(CloudBoard::getSerialCode).collect(Collectors.toSet());
        cloudBoardRepository.findAllByPiIdAndDeletedAtIsNull(piId).stream()
                .filter(board -> !registeredSerials.contains(board.getSerialCode()))
                .forEach(CloudBoard::markOffline);

        List<String> unknown = reported.keySet().stream().filter(s -> !registeredSerials.contains(s)).toList();
        return new HelloResult(false, unknown, reported);
    }

    // 등록된 보드면 true
    public boolean attach(Long piId, String serialCode, String usbSlot) {
        Optional<CloudBoard> board = cloudBoardRepository.findBySerialCodeAndDeletedAtIsNull(serialCode);
        board.ifPresent(b -> b.attach(hostPiRepository.getReferenceById(piId), usbSlot));
        return board.isPresent();
    }

    // 다른 파이로 옮겨 간 보드를 옛 파이의 보고로 끄지 않도록, 이 파이에 있는 보드만 끈다.
    public Optional<Long> detach(Long piId, String serialCode) {
        return cloudBoardRepository.findBySerialCodeAndDeletedAtIsNull(serialCode)
                .filter(b -> b.getPi().getId().equals(piId))
                .map(b -> {
                    b.markOffline();
                    return b.getId();
                });
    }

    public void markPiOffline(Long piId) {
        cloudBoardRepository.findAllByPiIdAndDeletedAtIsNull(piId).forEach(CloudBoard::markOffline);
    }

    public int markAllOffline() {
        return cloudBoardRepository.markAllOffline();
    }
}
