package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientFoodSuggestionResponse {
    private Long profileId;
    private String profileName;
    private Integer profileAgeMonths;
    private Boolean imageAnalyzed;
    private List<DetectedIngredientResponse> detectedIngredients;
    private List<MatchedFoodIngredientResponse> matchedDbIngredients;
    private List<String> inputIngredients;
    private List<String> normalizedIngredients;
    private List<IngredientFoodSuggestionItemResponse> suggestions;
    private List<String> warnings;
    private String disclaimer;
}
