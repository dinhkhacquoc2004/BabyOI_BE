package com.example.babyoi_be.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Profile name is required")
    @Size(min = 2, max = 50, message = "Profile name must be between 2 and 50 characters")
    private String name;

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth cannot be in the future")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateOfBirth;

    private String sex; // MALE, FEMALE, OTHER

    @NotBlank(message = "Profile type is required")
    private String profileType; // MOTHER, CHILD

    private String profileCode;
    private String functionCode;
    private String imageUrl;
}
