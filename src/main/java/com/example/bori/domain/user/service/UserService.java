package com.example.bori.domain.user.service;

import com.example.bori.domain.user.controller.dto.UserInfoResponse;
import com.example.bori.domain.user.domain.User;
import com.example.bori.domain.user.domain.UserRepository;
import com.example.bori.domain.user.exception.UserNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return UserInfoResponse.from(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
