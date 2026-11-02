package com.example.bori.domain.pi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "host_pi")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HostPi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "serial_code", nullable = false, length = 128, unique = true)
    private String serialCode;

    // 연결 토큰의 SHA-256(hex). 원문은 발급할 때 한 번만 보여준다.
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    // 마지막으로 L2가 연결(hello)한 시각
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public HostPi(String name, String serialCode, String tokenHash) {
        this.name = name;
        this.serialCode = serialCode;
        this.tokenHash = tokenHash;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public void markUsed(LocalDateTime at) {
        this.lastUsedAt = at;
    }
}
