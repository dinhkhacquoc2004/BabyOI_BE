package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {
    Optional<AuthRefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Modifying
    @Query("update AuthRefreshToken t set t.revokedAt = :revokedAt where t.user.id = :userId and t.revokedAt is null")
    int revokeAllActiveByUserId(Long userId, LocalDateTime revokedAt);
}
