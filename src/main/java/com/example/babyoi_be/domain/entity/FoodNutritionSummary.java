package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_nutrition_summary")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNutritionSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id")
    private FoodLibrary foodLibrary;

    @Column(name = "total_calories")
    private Double totalCalories;

    @Column(name = "total_calories_unit")
    private String totalCaloriesUnit;

    @Column(name = "total_protein")
    private Double totalProtein;

    @Column(name = "total_protein_unit")
    private String totalProteinUnit;

    @Column(name = "total_carbs")
    private Double totalCarbs;

    @Column(name = "total_carbs_unit")
    private String totalCarbsUnit;

    @Column(name = "total_fat")
    private Double totalFat;

    @Column(name = "total_fat_unit")
    private String totalFatUnit;

    @Column(name = "total_fiber")
    private Double totalFiber;

    @Column(name = "total_fiber_unit")
    private String totalFiberUnit;

    @Column(name = "total_sugar")
    private Double totalSugar;

    @Column(name = "total_sugar_unit")
    private String totalSugarUnit;

    @Column(name = "total_sodium")
    private Double totalSodium;

    @Column(name = "total_sodium_unit")
    private String totalSodiumUnit;
}
