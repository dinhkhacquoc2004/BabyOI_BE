package com.example.babyoi_be.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientFoodSuggestionRequest {
    private Long profileId;
    private List<String> ingredientNames;
    private String imageBase64;
    private String imageMimeType;
    private String imageFilename;
    private Integer limit;
}
