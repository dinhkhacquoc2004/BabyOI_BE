package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "food_library")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodLibrary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "function_code")
    private Long functionCode;

    private String name;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "advance_for", length = 100)
    private String advanceFor;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    private Long status;

    @OneToMany(mappedBy = "foodLibrary", cascade = CascadeType.ALL)
    private List<FoodLibraryIngredient> ingredients;

    @OneToOne(mappedBy = "foodLibrary", cascade = CascadeType.ALL)
    private FoodNutritionSummary nutritionSummary;

    @OneToOne(mappedBy = "foodLibrary", cascade = CascadeType.ALL)
    private FoodRecommendation recommendation;
}
