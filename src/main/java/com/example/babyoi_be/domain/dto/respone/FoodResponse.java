package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodResponse {
    private Long id;
    private Long functionCode;
    private String name;
    private String imageUrl;
    private String advanceFor;
    private Long status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private FoodNutritionResponse nutrition;
    private FoodRecommendationResponse recommendation;
    private List<FoodIngredientResponse> ingredients;
}
