package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.MiniappUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MiniappUserRepository extends JpaRepository<MiniappUser, Long> {

    Optional<MiniappUser> findByOpenid(String openid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from MiniappUser u where u.id = :id")
    Optional<MiniappUser> findByIdForUpdate(@Param("id") Long id);
}
