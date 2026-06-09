package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "package_structures",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_package_structures_order",
                        columnNames = {"package_id", "duration_months", "vaccine_id", "recommended_age_months", "dosage_order"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageStructure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private VaccinePackage vaccinePackage;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_id", nullable = false)
    private Vaccine vaccine;

    @Column(name = "recommended_age_months", nullable = false)
    private Integer recommendedAgeMonths;

    @Column(name = "dosage_order", nullable = false)
    private Integer dosageOrder;

    @Column(name = "dose_label")
    private String doseLabel;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
