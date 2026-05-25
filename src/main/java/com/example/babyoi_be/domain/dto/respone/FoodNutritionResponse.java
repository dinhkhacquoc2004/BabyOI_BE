package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNutritionResponse {
    private Long id;
    private Double totalCalories;
    private String totalCaloriesUnit;
    private Double totalProtein;
    private String totalProteinUnit;
    private Double totalCarbs;
    private String totalCarbsUnit;
    private Double totalFat;
    private String totalFatUnit;
    private Double totalFiber;
    private String totalFiberUnit;
    private Double totalSugar;
    private String totalSugarUnit;
    private Double totalSodium;
    private String totalSodiumUnit;
}
