package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "child_disease_dose_vaccine_options",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_child_disease_dose_vaccine_option", columnNames = {"disease_id", "dose_order", "vaccine_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDiseaseDoseVaccineOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_id", nullable = false)
    private ChildVaccineDisease disease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dose_schedule_id")
    private ChildDiseaseDoseSchedule doseSchedule;

    @Column(name = "dose_order", nullable = false)
    private Integer doseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_id", nullable = false)
    private Vaccine vaccine;

    @Column(name = "is_preferred")
    private Boolean preferred;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "status")
    private Long status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
