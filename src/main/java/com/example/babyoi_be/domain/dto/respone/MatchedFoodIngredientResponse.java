package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedFoodIngredientResponse {
    private String detectedName;
    private Double confidence;
    private Long ingredientId;
    private String ingredientName;
    private String imageUrl;
    private IngredientNutritionResponse nutrition;
}
