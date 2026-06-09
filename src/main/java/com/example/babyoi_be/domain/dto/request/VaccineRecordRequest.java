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

    private Long diseaseId;
    private Integer doseOrder;
    private Long source;
    private Long vaccineId;

    private LocalDate injectionDate;
    private LocalDate actualInjectionDate;
    private BigDecimal price;
    private String note;
    private Long status;
}
