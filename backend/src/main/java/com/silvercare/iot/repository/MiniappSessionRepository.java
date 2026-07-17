package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.MiniappSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface MiniappSessionRepository extends JpaRepository<MiniappSession, Long> {

    Optional<MiniappSession> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
}
