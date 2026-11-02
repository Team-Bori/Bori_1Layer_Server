package com.example.bori.domain.adminlog.service;

import com.example.bori.domain.adminlog.domain.AdminAction;
import com.example.bori.domain.adminlog.domain.AdminActionLog;
import com.example.bori.domain.adminlog.domain.AdminActionLogRepository;
import com.example.bori.domain.adminlog.domain.TargetType;
import com.example.bori.domain.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminActionLogService {

    private final AdminActionLogRepository adminActionLogRepository;
    private final UserRepository userRepository;

    // 조작과 같은 트랜잭션에서 남긴다. 조작이 롤백되면 기록도 남지 않는다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long adminId, AdminAction action, TargetType targetType, Long targetId, String detail) {
        adminActionLogRepository.save(new AdminActionLog(
                userRepository.getReferenceById(adminId), action, targetType, targetId, detail));
    }
}
