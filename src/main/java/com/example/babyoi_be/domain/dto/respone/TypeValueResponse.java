package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeValueResponse {
    private Long id;
    private String typeCode;
    private String valueCode;
    private String valueName;
    private String valueText;
    private BigDecimal valueNumber;
    private String description;
    private Integer sortOrder;
    private Long status;
}
