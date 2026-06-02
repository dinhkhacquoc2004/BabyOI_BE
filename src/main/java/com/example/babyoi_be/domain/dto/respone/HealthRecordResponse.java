package com.example.babyoi_be.domain.dto.respone;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordResponse {
    private Long id;
    private Long profileId;
    private Double height;
    private Double weight;
    private Double bmi;
    private Long illnessCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
