package com.example.babyoi_be.domain.dto.respone;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEntryResponse {
    private Long id;
    private Long profileId;
    private String title;
    private String content;
    private String imageUrl;
    private String milestone;
    private Long status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
