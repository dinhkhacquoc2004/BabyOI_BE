package com.example.babyoi_be.domain.dto.respone;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private Long userId;
    private String name;
    private LocalDate dateOfBirth;
    private String sex;
    private String profileType;
    private String profileCode;
    private String functionCode;
    private Long status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
