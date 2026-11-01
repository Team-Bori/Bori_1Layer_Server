package com.example.bori.global.security;

import com.example.bori.domain.user.domain.Role;
import com.example.bori.domain.user.domain.User;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomUserDetails implements UserDetails, CredentialsContainer {

    private final Long userId;
    private final String email;
    private String password;
    private final Role role;

    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    // 인증이 끝나면 세션에 비밀번호 해시가 남지 않도록 지운다.
    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
