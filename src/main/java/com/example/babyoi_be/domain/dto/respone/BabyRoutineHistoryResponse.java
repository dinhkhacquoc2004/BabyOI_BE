package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BabyRoutineHistoryResponse {
    private Long profileId;
    private String profileName;
    private String fromDate;
    private String toDate;
    private Integer totalDays;
    private List<BabyRoutineDayResponse> days;
}
