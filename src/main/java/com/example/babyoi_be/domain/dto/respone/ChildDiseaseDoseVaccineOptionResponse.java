package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDiseaseDoseVaccineOptionResponse {
    private Long id;
    private Long diseaseId;
    private Integer doseOrder;
    private Long vaccineId;
    private String vaccineName;
    private String manufacturer;
    private String origin;
    private String description;
    private Boolean preferred;
    private Integer displayOrder;
    private String note;
    private Long status;
}
