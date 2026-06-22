package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdAndArchivedAtIsNullAndStatusNotOrderByCreatedAtDesc(Long userId, Long status, Pageable pageable);

    Page<Notification> findByUserIdAndArchivedAtIsNotNullAndStatusNotOrderByCreatedAtDesc(Long userId, Long status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, Long status);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    Optional<Notification> findByReminderKey(String reminderKey);

    @Modifying
    @Query(value = """
            insert into notification
                (user_id, type, title, body, data_json, priority, source_type, source_id, reminder_key, status, created_at)
            values
                (:userId, :type, :title, :body, :dataJson, :priority, :sourceType, :sourceId, :reminderKey, :status, CURRENT_TIMESTAMP)
            on conflict (reminder_key) where reminder_key is not null do nothing
            """, nativeQuery = true)
    int insertReminder(Long userId, String type, String title, String body, String dataJson, Long priority,
                       String sourceType, Long sourceId, String reminderKey, Long status);

    @Modifying
    @Query("update Notification n set n.status = :readStatus, n.readAt = :readAt " +
            "where n.user.id = :userId and n.status = :unreadStatus and n.archivedAt is null")
    int markAllRead(Long userId, Long unreadStatus, Long readStatus, LocalDateTime readAt);

    @Modifying
    @Query("update Notification n set n.archivedAt = :archivedAt " +
            "where n.readAt is not null and n.archivedAt is null and n.readAt < :cutoff")
    int archiveReadBefore(LocalDateTime cutoff, LocalDateTime archivedAt);

    @Modifying
    @Query("delete from Notification n where n.archivedAt is not null and n.archivedAt < :cutoff")
    int deleteArchivedBefore(LocalDateTime cutoff);

    void deleteByUserId(Long userId);
}
