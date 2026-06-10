package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineProgressDoseResponse {
    private Long recordId;
    private Integer doseOrder;
    private Long source;
    private String sourceCode;
    private Long vaccineId;
    private String vaccineName;
    private String manufacturer;
    private LocalDate injectionDate;
    private LocalDate actualInjectionDate;
    private Long status;
    private String note;
}
