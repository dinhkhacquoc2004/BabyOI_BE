package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientNutritionResponse {
    private Long id;
    private Double baseAmount;
    private String baseUnit;
    private Double calories;
    private String caloriesUnit;
    private Double protein;
    private String proteinUnit;
    private Double carbs;
    private String carbsUnit;
    private Double fat;
    private String fatUnit;
    private Double fiber;
    private String fiberUnit;
    private Double sugar;
    private String sugarUnit;
    private Double sodium;
    private String sodiumUnit;
    private Long status;
}
