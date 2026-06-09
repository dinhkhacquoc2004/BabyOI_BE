package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildVaccineDiseaseResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean required;
    private Integer displayOrder;
    private Long status;
}
