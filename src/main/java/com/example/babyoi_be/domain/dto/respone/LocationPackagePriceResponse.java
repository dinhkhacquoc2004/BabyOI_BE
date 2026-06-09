package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationPackagePriceResponse {
    private Long id;
    private Long locationId;
    private String locationName;
    private Long packageId;
    private String packageCode;
    private String packageName;
    private Integer durationMonths;
    private BigDecimal baseVaccineSum;
    private BigDecimal serviceFee;
    private BigDecimal discountAmount;
    private BigDecimal finalPackagePrice;
    private String giftDescription;
}
