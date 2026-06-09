package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDiseasePlanResponse {
    private ChildVaccineDiseaseResponse disease;
    private List<ChildDiseaseDosePlanResponse> doses;
    private List<VaccineDiseaseCoverageResponse> coverages;
}
