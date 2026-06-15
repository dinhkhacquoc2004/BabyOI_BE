package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BabyRoutineEntryResponse {
    private Long id;
    private Long profileId;
    private String routineDate;
    private String type;
    private String time;
    private String actualTime;
    private String activity;
    private String note;
    private String icon;
    private String color;
    private Boolean completed;
    private String source;
}
