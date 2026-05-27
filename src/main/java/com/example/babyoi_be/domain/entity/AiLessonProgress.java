package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_lesson_progress")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiLessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private AiTeachingLesson lesson;

    @Column(name = "progress_status", nullable = false)
    private Long progressStatus;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @Column(name = "current_day")
    private Integer currentDay;

    @Column(name = "practice_count")
    private Integer practiceCount;

    @Column(name = "last_practiced_at")
    private LocalDate lastPracticedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    private Long status;
}
