package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "nutrition_plan")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "current_goal", columnDefinition = "TEXT")
    private String currentGoal;

    @Column(name = "future_goal", columnDefinition = "TEXT")
    private String futureGoal;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "meals_per_day", nullable = false)
    private Integer mealsPerDay;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "warnings_json", columnDefinition = "TEXT")
    private String warningsJson;

    @Column(name = "status")
    private Long status;

    @Column(name = "ai_model")
    private String aiModel;

    @Column(name = "ai_prompt_version")
    private String aiPromptVersion;

    @Column(name = "request_hash", length = 128)
    private String requestHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("planDate ASC, dayIndex ASC")
    private List<NutritionPlanDay> days;
}
