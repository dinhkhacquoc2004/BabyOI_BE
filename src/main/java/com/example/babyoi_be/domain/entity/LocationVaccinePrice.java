package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "location_vaccine_prices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_location_vaccine_prices", columnNames = {"location_id", "vaccine_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationVaccinePrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_id", nullable = false)
    private Vaccine vaccine;

    @Column(name = "retail_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal retailPrice;

    @Column(name = "stock_status")
    private Boolean stockStatus;

    @Column(name = "status")
    private Long status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
