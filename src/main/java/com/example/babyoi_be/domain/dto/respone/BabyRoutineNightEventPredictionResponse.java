package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BabyRoutineNightEventPredictionResponse {
    private String type;
    private String label;
    private String earliestDateTime;
    private String likelyDateTime;
    private String latestDateTime;
    private String note;
}
