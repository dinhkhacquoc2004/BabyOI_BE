package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "location_package_prices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_location_package_prices", columnNames = {"location_id", "package_id", "duration_months"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationPackagePrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private VaccinePackage vaccinePackage;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "base_vaccine_sum", precision = 19, scale = 2)
    private BigDecimal baseVaccineSum;

    @Column(name = "service_fee", precision = 19, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_package_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal finalPackagePrice;

    @Column(name = "gift_description", columnDefinition = "TEXT")
    private String giftDescription;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
