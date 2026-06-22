package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.NutritionPlanGenerateRequest;
import com.example.babyoi_be.domain.dto.request.NutritionPlanMealUpdateRequest;
import com.example.babyoi_be.domain.dto.respone.NutritionPlanResponse;

import java.util.List;

public interface NutritionPlanService {
    NutritionPlanResponse generatePlan(NutritionPlanGenerateRequest request);

    List<NutritionPlanResponse> getPlanHistory(Long profileId);

    NutritionPlanResponse updateMeal(Long mealId, NutritionPlanMealUpdateRequest request);

    NutritionPlanResponse completePlan(Long planId);

    void deletePlan(Long planId);
}
