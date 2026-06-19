package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.NutritionPlanGenerateRequest;
import com.example.babyoi_be.domain.dto.respone.NutritionPlanResponse;
import com.example.babyoi_be.service.NutritionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nutrition-plans")
@RequiredArgsConstructor
public class NutritionPlanController {

    private final NutritionPlanService nutritionPlanService;

    @PostMapping("/generate")
    public NutritionPlanResponse generatePlan(@Valid @RequestBody NutritionPlanGenerateRequest request) {
        return nutritionPlanService.generatePlan(request);
    }
}
