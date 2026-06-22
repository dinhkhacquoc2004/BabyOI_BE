package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NutritionPlanResponse {
    private String status;
    private String lifecycleStatus;
    private Long planId;
    private Long profileId;
    private String profileName;
    private String profileType;
    private String currentGoal;
    private String goalCode;
    private String userNotes;
    private Double targetDailyCalories;
    private String startDate;
    private String endDate;
    private Integer mealsPerDay;
    private String summary;
    private List<String> warnings;
    private String aiModel;
    private String aiPromptVersion;
    private Boolean cached;
    private List<NutritionPlanDayResponse> days;
}
