package com.example.babyoi_be.domain.dto.respone;

import lombok.*;
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
    private LocalDate injectionDate;
    private String location;
    private String note;
    private Long status;
}
