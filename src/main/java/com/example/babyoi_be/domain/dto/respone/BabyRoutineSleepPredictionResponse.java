package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BabyRoutineSleepPredictionResponse {
    private String sleepType;
    private String sleepStartDateTime;
    private String earliestWakeDateTime;
    private String likelyWakeDateTime;
    private String latestWakeDateTime;
    private Integer expectedSleepMinutesMin;
    private Integer expectedSleepMinutesLikely;
    private Integer expectedSleepMinutesMax;
    private Long minutesUntilLikelyWake;
    private List<BabyRoutineNightEventPredictionResponse> nightEvents;
    private String explanation;
}
