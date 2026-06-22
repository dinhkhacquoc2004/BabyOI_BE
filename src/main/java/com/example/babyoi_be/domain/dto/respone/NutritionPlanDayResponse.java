package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NutritionPlanDayResponse {
    private Long id;
    private String date;
    private Integer dayIndex;
    private String note;
    private List<NutritionPlanMealResponse> meals;
}
