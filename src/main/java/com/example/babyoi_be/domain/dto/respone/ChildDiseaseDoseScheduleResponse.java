package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDiseaseDoseScheduleResponse {
    private Long id;
    private Long diseaseId;
    private String diseaseCode;
    private String diseaseName;
    private Integer doseOrder;
    private Integer recommendedAgeMonths;
    private Integer intervalDays;
    private String doseLabel;
    private String note;
    private Long status;
}
