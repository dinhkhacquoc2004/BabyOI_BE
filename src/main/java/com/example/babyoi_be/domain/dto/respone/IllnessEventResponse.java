package com.example.babyoi_be.domain.dto.respone;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IllnessEventResponse {
    private Long id;
    private Long profileId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startAt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endAt;

    private String illnessType;
    private Long status;
    private String statusLabel;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
