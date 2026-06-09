package com.example.babyoi_be.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineRecordRequest {
    @NotNull(message = "Profile ID is required")
    private Long profileId;

    private Long locationId;
    private Long vaccineId;
    private Long packageId;
    private Long packageStructureId;

    private LocalDate injectionDate;
    private LocalDate actualInjectionDate;
    private BigDecimal price;
    private String note;
    private Long status;
}
