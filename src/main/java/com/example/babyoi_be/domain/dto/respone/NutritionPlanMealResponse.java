package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class NutritionPlanMealResponse {
    private Long id;
    private String mealType;
    private Long foodId;
    private String foodName;
    private String foodImageUrl;
    private FoodNutritionResponse nutrition;
    private Double adjustedCalories;
    private Double portionScale;
    private String ingredientStorageKey;
    private Map<String, String> ingredientAmounts;
    private String portion;
    private String reason;
    private String warning;
    private String eatenStatus;
}
