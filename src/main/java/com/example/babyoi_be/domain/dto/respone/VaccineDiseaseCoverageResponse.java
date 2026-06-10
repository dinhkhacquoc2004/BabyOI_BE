package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineDiseaseCoverageResponse {
    private Long id;
    private Long vaccineId;
    private String vaccineName;
    private String manufacturer;
    private Long diseaseId;
    private String diseaseCode;
    private String diseaseName;
    private String productFamilyCode;
    private Long interchangeRule;
    private String interchangeRuleCode;
    private Long status;
}
