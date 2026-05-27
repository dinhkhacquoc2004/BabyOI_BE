package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ai_teaching_lesson")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTeachingLesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_name", nullable = false)
    private String lessonName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reasonable_age_from_month")
    private Integer reasonableAgeFromMonth;

    @Column(name = "reasonable_age_to_month")
    private Integer reasonableAgeToMonth;

    @Column(name = "recommended_duration_days")
    private Integer recommendedDurationDays;

    @Column(name = "lesson_note", columnDefinition = "TEXT")
    private String lessonNote;

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "suggested_by_organization")
    private String suggestedByOrganization;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source_organization")
    private String sourceOrganization;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    private String iconUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    private Long status;

    @OneToMany(mappedBy = "lesson")
    private List<AiLessonProgress> progressRecords;
}
