package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.NutritionPlanGenerateRequest;
import com.example.babyoi_be.domain.dto.respone.NutritionPlanResponse;

public interface NutritionPlanService {
    NutritionPlanResponse generatePlan(NutritionPlanGenerateRequest request);
}
