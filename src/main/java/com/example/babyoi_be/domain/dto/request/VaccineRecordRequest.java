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

    @NotNull(message = "Vaccine type ID is required")
    private Long vaccineTypeId;

    private LocalDate injectionDate;
    private String location;
    private BigDecimal price;
    private String note;
    private Long status;
}
