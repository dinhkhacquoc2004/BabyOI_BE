package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nutrition_plan_meal")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionPlanMeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_day_id", nullable = false)
    private NutritionPlanDay planDay;

    @Column(name = "meal_type", nullable = false, length = 40)
    private String mealType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_library_id", nullable = false)
    private FoodLibrary foodLibrary;

    @Column(name = "food_name_snapshot", nullable = false)
    private String foodNameSnapshot;

    @Column(name = "portion")
    private String portion;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "warning", columnDefinition = "TEXT")
    private String warning;

    @Column(name = "eaten_status", nullable = false, length = 30)
    private String eatenStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
