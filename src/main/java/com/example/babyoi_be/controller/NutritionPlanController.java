package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.NutritionPlanGenerateRequest;
import com.example.babyoi_be.domain.dto.request.NutritionPlanMealUpdateRequest;
import com.example.babyoi_be.domain.dto.respone.NutritionPlanResponse;
import com.example.babyoi_be.service.NutritionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nutrition-plans")
@RequiredArgsConstructor
public class NutritionPlanController {

    private final NutritionPlanService nutritionPlanService;

    @PostMapping("/generate")
    public NutritionPlanResponse generatePlan(@Valid @RequestBody NutritionPlanGenerateRequest request) {
        return nutritionPlanService.generatePlan(request);
    }

    @GetMapping("/profile/{profileId}/history")
    public List<NutritionPlanResponse> getPlanHistory(@PathVariable Long profileId) {
        return nutritionPlanService.getPlanHistory(profileId);
    }

    @PatchMapping("/meals/{mealId}")
    public NutritionPlanResponse updateMeal(
            @PathVariable Long mealId,
            @RequestBody NutritionPlanMealUpdateRequest request) {
        return nutritionPlanService.updateMeal(mealId, request);
    }

    @PatchMapping("/{planId}/complete")
    public NutritionPlanResponse completePlan(@PathVariable Long planId) {
        return nutritionPlanService.completePlan(planId);
    }

    @DeleteMapping("/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlan(@PathVariable Long planId) {
        nutritionPlanService.deletePlan(planId);
    }
}
