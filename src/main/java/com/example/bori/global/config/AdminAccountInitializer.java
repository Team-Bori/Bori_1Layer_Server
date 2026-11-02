package com.example.bori.global.config;

import com.example.bori.domain.user.domain.User;
import com.example.bori.domain.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bori.admin.email}")
    private String email;

    @Value("${bori.admin.password}")
    private String password;

    @Value("${bori.admin.name}")
    private String name;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD가 없어 관리자 시드를 건너뛴다.");
            return;
        }
        // 이미 있으면 비밀번호를 덮어쓰지 않는다.
        if (userRepository.existsByEmail(email)) {
            return;
        }
        userRepository.save(User.createAdmin(email, passwordEncoder.encode(password), name));
        log.info("관리자 계정을 만들었다: {}", email);
    }
}
