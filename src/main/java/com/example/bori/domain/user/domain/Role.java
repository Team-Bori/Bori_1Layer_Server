package com.example.bori.domain.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    STUDENT("ROLE_STUDENT"),
    ADMIN("ROLE_ADMIN");

    private final String authority;
}
