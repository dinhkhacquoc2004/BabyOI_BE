package com.example.babyoi_be.domain.dto.request;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {
    private Long userId;
    private String name;
    private LocalDate dateOfBirth;
    private String sex; // MALE, FEMALE, OTHER
    private String profileType; // MOTHER, CHILD
    private String profileCode;
    private String functionCode;
}
