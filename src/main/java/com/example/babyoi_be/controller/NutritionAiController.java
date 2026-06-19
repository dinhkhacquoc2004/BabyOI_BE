package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.IngredientAdjustmentSuggestRequest;
import com.example.babyoi_be.domain.dto.respone.IngredientAdjustmentSuggestionResponse;
import com.example.babyoi_be.service.NutritionAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nutrition-ai")
@RequiredArgsConstructor
public class NutritionAiController {

    private final NutritionAiService nutritionAiService;

    @PostMapping("/ingredient-adjustments/suggest")
    public IngredientAdjustmentSuggestionResponse suggestIngredientAdjustments(@Valid @RequestBody IngredientAdjustmentSuggestRequest request) {
        return nutritionAiService.suggestIngredientAdjustments(request);
    }
}
