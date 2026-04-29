package com.example.babyoi_be.domain.dto.respone;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccinationCenterResponse {
    private Long id;
    private String name;
    private String address;
    private Double distance;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String note;
}
