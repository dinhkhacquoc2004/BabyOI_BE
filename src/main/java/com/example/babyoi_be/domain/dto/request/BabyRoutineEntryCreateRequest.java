package com.example.babyoi_be.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BabyRoutineEntryCreateRequest {
    @NotBlank
    private String routineDate;

    @NotBlank
    private String plannedTime;

    @NotBlank
    private String activity;

    private String type;
    private String note;
    private String icon;
    private String color;
}
