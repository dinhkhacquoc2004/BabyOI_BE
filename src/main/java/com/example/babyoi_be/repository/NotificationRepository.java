package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, Long status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, Long status);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
