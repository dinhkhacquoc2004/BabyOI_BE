package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageStructureResponse {
    private Long id;
    private Long packageId;
    private String packageName;
    private Integer durationMonths;
    private Long vaccineId;
    private String vaccineName;
    private Integer recommendedAgeMonths;
    private Integer dosageOrder;
    private String doseLabel;
    private String note;
}
