package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.MiniappUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MiniappUserRepository extends JpaRepository<MiniappUser, Long> {

    Optional<MiniappUser> findByOpenid(String openid);
}
