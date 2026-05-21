package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ingredient_nutrition")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientNutrition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_ingredient_id")
    private FoodIngredient foodIngredient;

    @Column(name = "base_amount")
    private Double baseAmount;

    @Column(name = "base_unit")
    private String baseUnit;

    private Double calories;

    @Column(name = "calories_unit")
    private String caloriesUnit;

    private Double protein;

    @Column(name = "protein_unit")
    private String proteinUnit;

    private Double carbs;

    @Column(name = "carbs_unit")
    private String carbsUnit;

    private Double fat;

    @Column(name = "fat_unit")
    private String fatUnit;

    private Double fiber;

    @Column(name = "fiber_unit")
    private String fiberUnit;

    private Double sugar;

    @Column(name = "sugar_unit")
    private String sugarUnit;

    private Double sodium;

    @Column(name = "sodium_unit")
    private String sodiumUnit;

    private Long status;
}
