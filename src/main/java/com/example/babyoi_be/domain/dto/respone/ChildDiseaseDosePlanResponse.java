package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDiseaseDosePlanResponse {
    private ChildDiseaseDoseScheduleResponse schedule;
    private List<ChildDiseaseDoseVaccineOptionResponse> vaccineOptions;
}
