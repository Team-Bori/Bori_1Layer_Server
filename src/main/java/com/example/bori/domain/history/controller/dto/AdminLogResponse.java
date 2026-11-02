package com.example.bori.domain.history.controller.dto;

import com.example.bori.domain.adminlog.domain.AdminAction;
import com.example.bori.domain.adminlog.domain.AdminActionLog;
import com.example.bori.domain.adminlog.domain.TargetType;
import java.time.LocalDateTime;

public record AdminLogResponse(
        Long id,
        LocalDateTime createdAt,
        String adminName,
        AdminAction action,
        TargetType targetType,
        Long targetId,
        String detail
) {

    public static AdminLogResponse from(AdminActionLog l) {
        return new AdminLogResponse(l.getId(), l.getCreatedAt(), l.getAdmin().getName(), l.getAction(), l.getTargetType(),
                l.getTargetId(), l.getDetail());
    }
}
