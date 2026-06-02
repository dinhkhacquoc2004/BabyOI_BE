package com.example.babyoi_be.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordRequest {
    @NotNull(message = "Profile ID is required")
    private Long profileId;

    @Positive(message = "Height must be greater than 0")
    private Double height;

    @Positive(message = "Weight must be greater than 0")
    private Double weight;

    @Positive(message = "BMI must be greater than 0")
    private Double bmi;

    @NotNull(message = "Record date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;
}
