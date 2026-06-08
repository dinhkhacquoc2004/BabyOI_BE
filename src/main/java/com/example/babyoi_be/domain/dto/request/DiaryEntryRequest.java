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
public class DiaryEntryRequest {
    @NotNull(message = "Profile ID is required")
    private Long profileId;

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must be at most 120 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must be at most 5000 characters")
    private String content;

    @Size(max = 1000, message = "Image URL must be at most 1000 characters")
    private String imageUrl;

    @Size(max = 100, message = "Milestone must be at most 100 characters")
    private String milestone;

    @NotNull(message = "Entry date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;
}
