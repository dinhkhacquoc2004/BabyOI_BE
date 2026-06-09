package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccinePackageResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Long status;
}
