package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_setting")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled;

    @Column(name = "vaccine_enabled", nullable = false)
    private Boolean vaccineEnabled;

    @Column(name = "appointment_enabled", nullable = false)
    private Boolean appointmentEnabled;

    @Column(name = "chat_enabled", nullable = false)
    private Boolean chatEnabled;

    @Column(name = "promotion_enabled", nullable = false)
    private Boolean promotionEnabled;

    @Column(name = "system_enabled", nullable = false)
    private Boolean systemEnabled;

    @Column(name = "vaccine_in_app_enabled", nullable = false)
    private Boolean vaccineInAppEnabled;

    @Column(name = "vaccine_push_enabled", nullable = false)
    private Boolean vaccinePushEnabled;

    @Column(name = "appointment_in_app_enabled", nullable = false)
    private Boolean appointmentInAppEnabled;

    @Column(name = "appointment_push_enabled", nullable = false)
    private Boolean appointmentPushEnabled;

    @Column(name = "chat_in_app_enabled", nullable = false)
    private Boolean chatInAppEnabled;

    @Column(name = "chat_push_enabled", nullable = false)
    private Boolean chatPushEnabled;

    @Column(name = "promotion_in_app_enabled", nullable = false)
    private Boolean promotionInAppEnabled;

    @Column(name = "promotion_push_enabled", nullable = false)
    private Boolean promotionPushEnabled;

    @Column(name = "system_in_app_enabled", nullable = false)
    private Boolean systemInAppEnabled;

    @Column(name = "system_push_enabled", nullable = false)
    private Boolean systemPushEnabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
