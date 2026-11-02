package com.example.bori.domain.pi.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HostPiRepository extends JpaRepository<HostPi, Long> {

    Optional<HostPi> findByTokenHash(String tokenHash);

    boolean existsBySerialCode(String serialCode);
}
