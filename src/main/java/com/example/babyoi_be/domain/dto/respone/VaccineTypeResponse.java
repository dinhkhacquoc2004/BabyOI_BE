package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineTypeResponse {
    private Long id;
    private String name;
    private String description;
    private String requiredAge;
}
