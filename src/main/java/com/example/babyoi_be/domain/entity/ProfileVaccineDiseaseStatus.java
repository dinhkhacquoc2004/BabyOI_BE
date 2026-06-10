package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "profile_vaccine_disease_status",
        uniqueConstraints = @UniqueConstraint(name = "uk_profile_vaccine_disease_status", columnNames = {"profile_id", "disease_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileVaccineDiseaseStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_id", nullable = false)
    private ChildVaccineDisease disease;

    @Column(name = "status", nullable = false)
    private Long status;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @Column(name = "stopped_dose_order")
    private Integer stoppedDoseOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
