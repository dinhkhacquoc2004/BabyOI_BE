package com.example.babyoi_be.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IllnessEventRequest {
    @NotNull(message = "Profile ID is required")
    private Long profileId;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startAt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endAt;

    @NotBlank(message = "Illness type is required")
    @Size(max = 255, message = "Illness type must be at most 255 characters")
    private String illnessType;

    private Long status;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;
}
