package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "child_disease_dose_schedules",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_child_disease_dose", columnNames = {"disease_id", "dose_order"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDiseaseDoseSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_id", nullable = false)
    private ChildVaccineDisease disease;

    @Column(name = "dose_order", nullable = false)
    private Integer doseOrder;

    @Column(name = "recommended_age_months")
    private Integer recommendedAgeMonths;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "dose_label")
    private String doseLabel;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "status")
    private Long status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
