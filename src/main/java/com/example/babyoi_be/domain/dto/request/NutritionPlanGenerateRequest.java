package com.example.babyoi_be.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NutritionPlanGenerateRequest {
    @NotNull
    private Long profileId;

    @NotBlank
    private String startDate;

    @NotBlank
    private String endDate;

    private Integer mealsPerDay;
    private List<String> allowedMealTypes;
    private String currentGoal;
    private String futureGoal;
    private String userNotes;
    private List<Long> candidateFoodIds;
}
