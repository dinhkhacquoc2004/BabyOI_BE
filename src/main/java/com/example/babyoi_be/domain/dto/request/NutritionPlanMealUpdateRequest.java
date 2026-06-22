package com.example.babyoi_be.domain.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NutritionPlanMealUpdateRequest {
    private String mealType;
    private Long foodId;
    private String portion;
    private String reason;
    private String warning;
    private String eatenStatus;
}
