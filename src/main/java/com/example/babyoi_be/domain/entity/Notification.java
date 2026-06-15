package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "type", nullable = false, length = 80)
    private String type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "data_json", columnDefinition = "TEXT")
    private String dataJson;

    @Column(name = "priority", nullable = false)
    private Long priority;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "status", nullable = false)
    private Long status;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "reminder_key", length = 160, unique = true)
    private String reminderKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
