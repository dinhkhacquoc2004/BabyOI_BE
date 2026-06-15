package com.example.babyoi_be.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BabyRoutineEntryUpdateRequest {
    @NotNull
    private Boolean completed;

    private String actualTime;
    private String note;
}
