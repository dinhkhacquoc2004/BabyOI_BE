package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.UserSessionToken;
import com.example.babyoi_be.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserSessionTokenRepository extends JpaRepository<UserSessionToken, Long> {
    Optional<UserSessionToken> findByTokenHash(String tokenHash);

    void deleteAllByUser(Users user);

    void deleteAllByExpiresAtBefore(Instant cutoff);
}
