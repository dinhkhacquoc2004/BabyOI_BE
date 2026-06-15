package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BabyRoutineAiAnalysisResponse {
    private String title;
    private String summary;
    private String recommendedTomorrowWakeTime;
    private List<String> highlights;
    private List<String> warnings;
    private List<String> suggestions;
    private String disclaimer;
}
