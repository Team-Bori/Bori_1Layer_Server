package com.example.bori.domain.pi.service;

import com.example.bori.domain.adminlog.domain.AdminAction;
import com.example.bori.domain.adminlog.domain.TargetType;
import com.example.bori.domain.adminlog.service.AdminActionLogService;
import com.example.bori.domain.pi.controller.dto.HostPiCreateRequest;
import com.example.bori.domain.pi.controller.dto.HostPiResponse;
import com.example.bori.domain.pi.controller.dto.HostPiTokenResponse;
import com.example.bori.domain.pi.domain.HostPi;
import com.example.bori.domain.pi.domain.HostPiRepository;
import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostPiService {

    private final HostPiRepository hostPiRepository;
    private final PiTokenGenerator tokenGenerator;
    private final AdminActionLogService adminActionLogService;

    public List<HostPiResponse> getPis() {
        return hostPiRepository.findAll(Sort.by("id")).stream()
                .map(HostPiResponse::from)
                .toList();
    }

    @Transactional
    public HostPiTokenResponse create(Long adminId, HostPiCreateRequest request) {
        if (hostPiRepository.existsBySerialCode(request.serialCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PI_SERIAL);
        }
        String token = tokenGenerator.newToken();
        HostPi pi = hostPiRepository.save(new HostPi(request.name(), request.serialCode(), tokenGenerator.hash(token)));
        adminActionLogService.record(adminId, AdminAction.CREATE, TargetType.HOST_PI, pi.getId(), pi.getName() + " 등록");
        return new HostPiTokenResponse(pi.getId(), pi.getName(), pi.getSerialCode(), token);
    }

    // 토큰을 잃어버렸거나 유출됐을 때. 이전 토큰은 바로 쓸 수 없게 된다 (이미 열린 연결은 유지된다).
    @Transactional
    public HostPiTokenResponse reissueToken(Long adminId, Long piId) {
        HostPi pi = getPi(piId);
        String token = tokenGenerator.newToken();
        pi.changeTokenHash(tokenGenerator.hash(token));
        adminActionLogService.record(adminId, AdminAction.TOKEN_REISSUE, TargetType.HOST_PI, pi.getId(), pi.getName() + " 토큰 재발급");
        return new HostPiTokenResponse(pi.getId(), pi.getName(), pi.getSerialCode(), token);
    }

    public HostPi getPi(Long piId) {
        return hostPiRepository.findById(piId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PI_NOT_FOUND));
    }
}
