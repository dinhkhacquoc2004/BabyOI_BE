package com.example.babyoi_be.domain.dto.respone;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineRecordResponse {
    private Long id;
    private Long profileId;
    private Long locationId;
    private String locationName;
    private Long vaccineId;
    private String vaccineName;
    private String vaccineDescription;
    private Long packageId;
    private String packageName;
    private Long packageStructureId;
    private Integer durationMonths;
    private Integer recommendedAgeMonths;
    private Integer dosageOrder;
    private LocalDate injectionDate;
    private LocalDate actualInjectionDate;
    private BigDecimal price;
    private String note;
    private Long status;
}
