package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.IngredientAdjustmentSuggestRequest;
import com.example.babyoi_be.domain.dto.respone.IngredientAdjustmentSuggestionResponse;

public interface NutritionAiService {
    IngredientAdjustmentSuggestionResponse suggestIngredientAdjustments(IngredientAdjustmentSuggestRequest request);
}
