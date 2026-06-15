package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BabyRoutineDayResponse {
    private Long profileId;
    private String profileName;
    private Integer profileAgeMonths;
    private String routineDate;
    private String ageGroup;
    private Integer sleepTargetMinHours;
    private Integer sleepTargetMaxHours;
    private String feedingGuide;
    private String activityGuide;
    private BabyRoutineSleepPredictionResponse sleepPrediction;
    private List<BabyRoutineEntryResponse> entries;
    private BabyRoutineAiAnalysisResponse aiInsight;
}
