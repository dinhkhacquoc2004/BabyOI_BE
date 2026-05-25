package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.AuthOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthOtpRepository extends JpaRepository<AuthOtp, Long> {
    Optional<AuthOtp> findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            String email,
            AuthOtp.Purpose purpose
    );

    void deleteByEmail(String email);
}
