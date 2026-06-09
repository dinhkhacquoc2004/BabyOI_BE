package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationVaccinePriceResponse {
    private Long id;
    private Long locationId;
    private String locationName;
    private Long vaccineId;
    private String vaccineName;
    private String manufacturer;
    private String origin;
    private BigDecimal retailPrice;
    private Boolean stockStatus;
    private Long status;
}
