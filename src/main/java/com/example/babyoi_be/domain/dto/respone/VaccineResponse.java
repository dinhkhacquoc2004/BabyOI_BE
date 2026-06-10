package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineResponse {
    private Long id;
    private String name;
    private String manufacturer;
    private String origin;
    private String description;
    private Long status;
}
