package com.example.bori.domain.user.controller.dto;

import com.example.bori.domain.user.domain.Role;
import com.example.bori.domain.user.domain.User;

public record UserInfoResponse(
        Long id,
        String email,
        String name,
        String studentNo,
        Role role
) {

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(user.getId(), user.getEmail(), user.getName(), user.getStudentNo(), user.getRole());
    }
}
