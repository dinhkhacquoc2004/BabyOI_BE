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
    private Long vaccineTypeId;
    private String vaccineTypeName;
    private String vaccineTypeDescription;
    private LocalDate injectionDate;
    private String location;
    private BigDecimal price;
    private String note;
    private Long status;
}
