package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientFoodSuggestionItemResponse {
    private FoodResponse food;
    private Double matchScore;
    private List<String> matchedIngredients;
    private List<String> missingIngredients;
    private String suitabilityNote;
    private String nutritionNote;
}
